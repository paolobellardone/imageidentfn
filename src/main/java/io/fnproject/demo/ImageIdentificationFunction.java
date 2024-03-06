/*
 *
 * MIT License
 *
 * Copyright (c) 2023-24 PaoloB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.fnproject.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.aivision.AIServiceVision;
import com.oracle.bmc.aivision.AIServiceVisionClient;
import com.oracle.bmc.aivision.model.*;
import com.oracle.bmc.aivision.requests.*;
import com.oracle.bmc.aivision.responses.*;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class that implements the image identification function.
 *
 * @version 1.4 6 Mar 2024
 * @author PaoloB
 */
public class ImageIdentificationFunction {

    // Variables to save the environment variables of the function
    private Boolean debug;    // DEBUG - Enables debugging informations in log files
    private String nameSpace; // OCI_NAMESPACE - Object Storage namespace
    private String bucketIn;  // BUCKET_IN - Bucket that emits events when an image is uploaded
    private String bucketOut; // BUCKET_OUT - Bucket used to store the results of the analysis

    // Variables to save the internal environment variables
    private String ociResourcePrincipalVersion; // OCI_RESOURCE_PRINCIPAL_VERSION
    private String ociResourcePrincipalRegion;  // OCI_RESOURCE_PRINCIPAL_REGION
    private String ociResourcePrincipalRPST;    // OCI_RESOURCE_PRINCIPAL_RPST
    private String ociResourcePrincipalPEM;     // OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM

    // Authentication using Resource Principal
    final ResourcePrincipalAuthenticationDetailsProvider provider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

    // Constants
    static final String ERRORMSG = "Error during the identification process, please check logs.";
    static final String POSTFIX = "-metadata.json";

    /**
     * Get the configuration fron the environment configured for the function
     *
     * @param ctx the runtime context of the function
     */
    @FnConfiguration
    public void config(RuntimeContext ctx) {

        // If true, the logging will be more verbose
        debug = Boolean.valueOf(ctx.getConfigurationByKey("DEBUG").orElse("false"));
        nameSpace = ctx.getConfigurationByKey("OCI_NAMESPACE").orElseThrow(() -> new RuntimeException("Missing configuration: OCI_NAMESPACE"));
        // Default value for BUCKET_IN is imageAI
        bucketIn = ctx.getConfigurationByKey("BUCKET_IN").orElse("imageAI");
        // Default value for BUCKET_OUT is imageAI
        bucketOut = ctx.getConfigurationByKey("BUCKET_OUT").orElse("imageAI");

        // OCI_RESOURCE_PRINCIPAL_VERSION
        ociResourcePrincipalVersion = ctx.getConfigurationByKey("OCI_RESOURCE_PRINCIPAL_VERSION").orElseThrow(() -> new RuntimeException("Missing configuration: OCI_RESOURCE_PRINCIPAL_VERSION"));
        // OCI_RESOURCE_PRINCIPAL_REGION
        ociResourcePrincipalRegion = ctx.getConfigurationByKey("OCI_RESOURCE_PRINCIPAL_REGION").orElseThrow(() -> new RuntimeException("Missing configuration: OCI_RESOURCE_PRINCIPAL_REGION"));
        // OCI_RESOURCE_PRINCIPAL_RPST
        ociResourcePrincipalRPST = ctx.getConfigurationByKey("OCI_RESOURCE_PRINCIPAL_RPST").orElseThrow(() -> new RuntimeException("Missing configuration: OCI_RESOURCE_PRINCIPAL_RPST"));
        // OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM
        ociResourcePrincipalPEM = ctx.getConfigurationByKey("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM").orElseThrow(() -> new RuntimeException("Missing configuration: OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM"));

    }

    /**
     * Image identification function. It reads an image from a bucket defined in OCI
     * Object Storage and then it identifies
     * the features and ontologies inside the image using the default AI model. The
     * result is saved in the same bucket.
     *
     * @param ctx          Oracle Functions runtime context
     * @param eventPayload Oracle Events payload
     * @return a message with the result of the operation invoked
     */
    public String handleRequest(RuntimeContext ctx, String eventPayload) {

        // Create a logger instance to print messages to System.err
        Logger logger = LoggerFactory.getLogger(ImageIdentificationFunction.class);

        // Print out some configuration details for debugging purposes
        if (Boolean.TRUE.equals(debug)) {
            logger.info("OCI_RESOURCE_PRINCIPAL_VERSION: {}", ociResourcePrincipalVersion);
            logger.info("OCI_RESOURCE_PRINCIPAL_REGION: {}", ociResourcePrincipalRegion);
            logger.info("OCI_RESOURCE_PRINCIPAL_RPST: {}", ociResourcePrincipalRPST);
            logger.info("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM: {}", ociResourcePrincipalPEM);
        }

        // Create a client to access the Object Storage service
        ObjectStorage objStorageClient = null;
        objStorageClient = ObjectStorageClient.builder().build(provider);

        // Check if the Object client is available, if not it exits with an error
        if (objStorageClient == null) {
            logger.error("There was a problem creating the ObjectStorageClient object. Please check logs.");
            return ERRORMSG;
        }

        // Create a client to access the AI Vision service
        AIServiceVision aiVisionClient = null;
        aiVisionClient = AIServiceVisionClient.builder().build(provider);

        // Check if the OCI client is available, if not exits with an error
        if (aiVisionClient == null) {
            logger.error("There was a problem creating the AIServiceVisionClient object. Please check logs.");
            return ERRORMSG;
        }

        // If the OCI-related parameters are not defined the function cannot proceed
        if (nameSpace.isEmpty() || bucketIn.isEmpty() || bucketOut.isEmpty()) {
            logger.error("The required environment variables OCI_NAMESPACE, BUCKET_IN, BUCKET_OUT are not defined. Please configure them before proceeding.");
            return ERRORMSG;
        }

        try {
            // Unmarshal the payload into a Java class
            ObjectMapper mapper = new ObjectMapper();
            ObjectStorageCloudEvent osCloudEvent = mapper.readValue(eventPayload, ObjectStorageCloudEvent.class);

            // Get the filename from the payload passed by Oracle Events
            String fileName = osCloudEvent.getData().get("resourceName").toString();

            // Get the file type, if it is an image then will be analyzed
            GetObjectResponse getObjectResponse = objStorageClient.getObject(GetObjectRequest.builder()
                                                                                             .namespaceName(nameSpace)
                                                                                             .bucketName(bucketIn)
                                                                                             .objectName(fileName)
                                                                                             .build());
            String fileType = (new StringTokenizer(getObjectResponse.getContentType(), "/")).nextToken();

            if (fileType.equals("image")) {

                logger.info("Analyzing file: {}", fileName);

                // Create an AnalyzeImageRequest and dependent object(s)
                AnalyzeImageDetails analyzeImageDetails = AnalyzeImageDetails.builder()
                                                                                .features(new ArrayList<>(
                                                                                        Arrays.asList(ImageClassificationFeature.builder().build())))
                                                                                .image(ObjectStorageImageDetails.builder()
                                                                                        .namespaceName(nameSpace)
                                                                                        .bucketName(bucketIn)
                                                                                        .objectName(fileName).build())
                                                                                .build();

                // Analyze the file for classification with AI Vision
                AnalyzeImageRequest analyzeImageRequest = AnalyzeImageRequest.builder().analyzeImageDetails(analyzeImageDetails).build();
                AnalyzeImageResponse analyzeImageResponse = aiVisionClient.analyzeImage(analyzeImageRequest);
                AnalyzeImageResult analyzeImageResult = analyzeImageResponse.getAnalyzeImageResult();

                // Configure the Jackson mapper to pretty print the results of the AI Vision analysis
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                FilterProvider filter = new SimpleFilterProvider().setFailOnUnknownId(false);
                ObjectWriter writer = mapper.writer(filter);

                // This file will contain the results of the analysis formatted in JSON
                String resultsFile = fileName + POSTFIX;

                logger.info("Writing results in file: {}", resultsFile);

                // Input and output streams
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ByteArrayInputStream is = new ByteArrayInputStream(writer.writeValueAsString(analyzeImageResult).getBytes());

                // Write the results in the resultsFile in object storage
                PutObjectResponse putObjectResponse = objStorageClient.putObject(PutObjectRequest.builder()
                                                                                                 .namespaceName(nameSpace)
                                                                                                 .bucketName(bucketOut)
                                                                                                 .objectName(resultsFile)
                                                                                                 .putObjectBody(is)
                                                                                                 .build());

                if (putObjectResponse == null) {
                    logger.error("Error creating results file: {}", resultsFile);
                    return ERRORMSG;
                } else {
                    logger.info("Created results file: {}", resultsFile);
                }

                // Close the streams
                is.close();
                os.close();

                // Close the OCI clients
                objStorageClient.close();
                aiVisionClient.close();

                logger.info("Image identification completed, please see the output in bucket {}", bucketOut);
                return "Image identification completed, please see the output in bucket " + bucketOut;

            } else {

                logger.error("This file is not an image or is not a supported format.");
                return "This file is not an image or is not a supported format.";

            }

        } catch (Exception e) {
            logger.error("Error during identification of image: {}", e.getMessage());
            return ERRORMSG;
        }

    }

}
