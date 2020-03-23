package edu.isi.oba.config;

import org.junit.Assert;
import org.junit.Test;

public class ProviderTest {
    @Test
    public void getSelectedClasses() {
        Provider provider = Provider.get("firebase");
        Provider provider_test = Provider.FIREBASE;
        Assert.assertEquals(provider_test, provider);
    }
}
