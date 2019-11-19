package edu.isi.oba.config;

public class EndpointConfig {
  public String url;
  public String prefix;
  public String graph_base;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getGraph_base() {
    return graph_base;
  }

  public void setGraph_base(String graph_base) {
    this.graph_base = graph_base;
  }
}
