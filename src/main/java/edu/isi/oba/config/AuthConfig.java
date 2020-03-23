package edu.isi.oba.config;

public class AuthConfig {
    public Boolean enable = false;
    private String provider;
    public Provider provider_obj;

    public Provider getProvider_obj() {
        return provider_obj;
    }

    public void setProvider_obj(Provider provider_obj) {
        this.provider_obj = provider_obj;
    }


    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
        this.provider_obj = Provider.get(provider);
        this.enable = true;
    }
}
