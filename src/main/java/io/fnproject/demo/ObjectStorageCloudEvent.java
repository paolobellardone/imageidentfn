/*
 *
 * MIT License
 *
 * Copyright (c) 2022-24 PaoloB
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

import java.util.Map;

public class ObjectStorageCloudEvent {
    private String cloudEventsVersion;
    private String eventID;
    private String eventType;
    private String eventTypeVersion;
    private String eventTime;
    private String contentType;
    private String source;
    private Map<String, Object> extensions;
    private Map<String, Object> data;

    public String getCloudEventsVersion() {
        return this.cloudEventsVersion;
    }

    public void setCloudEventsVersion(String cloudEventsVersion) {
        this.cloudEventsVersion = cloudEventsVersion;
    }

    public String getEventID() {
        return this.eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getEventType() {
        return this.eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTypeVersion() {
        return this.eventTypeVersion;
    }

    public void setEventTypeVersion(String eventTypeVersion) {
        this.eventTypeVersion = eventTypeVersion;
    }

    public String getEventTime() {
        return this.eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String,Object> getExtensions() {
        return this.extensions;
    }

    public void setExtensions(Map<String,Object> extensions) {
        this.extensions = extensions;
    }

    public Map<String,Object> getData() {
        return this.data;
    }

    public void setData(Map<String,Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "{" +
            " cloudEventsVersion='" + getCloudEventsVersion() + "'" +
            ", eventID='" + getEventID() + "'" +
            ", eventType='" + getEventType() + "'" +
            ", eventTypeVersion='" + getEventTypeVersion() + "'" +
            ", eventTime='" + getEventTime() + "'" +
            ", contentType='" + getContentType() + "'" +
            ", source='" + getSource() + "'" +
            ", extensions='" + getExtensions() + "'" +
            ", data='" + getData() + "'" +
            "}";
    }

}
