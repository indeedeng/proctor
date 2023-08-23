package com.indeed.proctor.consumer.logging;

import com.indeed.proctor.common.ProctorResult;

public interface ExposureLogger {

    void logExposureInfo(ProctorResult proctorResult, String testName);

    void logExposureInfo(ProctorResult proctorResult);
}
