package org.apache.karaf.features.internal;

import javax.xml.namespace.QName;

public interface FeaturesNamespace {

	String URI_0_0_0 = "";
	String URI_1_0_0 = "http://karaf.apache.org/xmlns/features/v1.0.0";
	String URI_1_1_0 = "http://karaf.apache.org/xmlns/features/v1.1.0";

	String URI_CURRENT = URI_1_1_0;

	QName FEATURES_0_0_0 = new QName("features");
	QName FEATURES_1_0_0 = new QName(URI_1_0_0, "features");
	QName FEATURES_1_1_0 = new QName(URI_1_1_0, "features");

	QName FEATURES_CURRENT = FEATURES_1_1_0;

}
