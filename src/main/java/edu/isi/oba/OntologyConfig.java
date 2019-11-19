package edu.isi.oba;

import java.util.List;


public class OntologyConfig {
  private String xmlUrl;
  private String prefix;
  private String prefixUri;

  public String getXmlUrl() {
    return xmlUrl;
  }

  public void setXmlUrl(String xmlUrl) {
    this.xmlUrl = xmlUrl;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefixUri() {
    return prefixUri;
  }

  public void setPrefixUri(String prefixUri) {
    this.prefixUri = prefixUri;
  }
}


