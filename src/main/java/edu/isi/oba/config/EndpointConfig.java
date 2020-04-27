package edu.isi.oba.config;

import edu.isi.oba.ObaUtils;

public class EndpointConfig {
  private String url;
  private String prefix;
  private String graph_base;

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
    this.prefix = ObaUtils.check_trailing_slash(prefix);
  }

  public String getGraph_base() {
    return graph_base;
  }

  public void setGraph_base(String graph_base) {
    this.graph_base =  ObaUtils.check_trailing_slash(graph_base);
  }
}
