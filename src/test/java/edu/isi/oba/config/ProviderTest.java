package edu.isi.oba.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProviderTest {
    @Test
    public void getSelectedClasses() {
        Provider provider = Provider.get("firebase");
        Provider provider_test = Provider.FIREBASE;
        Assertions.assertEquals(provider_test, provider);
    }
}
