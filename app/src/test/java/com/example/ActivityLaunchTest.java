package com.example;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ActivityLaunchTest {

    @Test
    public void testRepositoryAndManagersInitialization() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);

        StorageManager storageManager = StorageManager.getInstance(context);
        assertNotNull(storageManager);

        AccountingService accountingService = AccountingService.getInstance(context);
        assertNotNull(accountingService);

        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance(context);
        assertNotNull(authManager);

        MawaSyncManager syncManager = MawaSyncManager.getInstance(context);
        assertNotNull(syncManager);

        SupabaseClientConfig clientConfig = SupabaseClientConfig.getInstance(context);
        assertNotNull(clientConfig);
    }
}
