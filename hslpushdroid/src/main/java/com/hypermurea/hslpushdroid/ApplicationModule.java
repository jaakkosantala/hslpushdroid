package com.hypermurea.hslpushdroid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hypermurea.hslpushdroid.gcm.DevelopmentGCMRegistrationService;
import com.hypermurea.hslpushdroid.gcm.GCMIntentService;
import com.hypermurea.hslpushdroid.gcm.GCMRegistrationService;
import com.hypermurea.hslpushdroid.gcm.LiveGCMRegistrationService;
import com.hypermurea.hslpushdroid.reittiopas.FindLinesService;
import com.hypermurea.hslpushdroid.reittiopas.FindLinesServiceImpl;
import com.hypermurea.hslpushdroid.reittiopas.ReittiopasService;
import com.hypermurea.hslpushdroid.user.UserProfileFactory;

public class ApplicationModule extends AbstractModule {
	
	public static final String LAST_DISRUPTION_PREFERENCE = "last_disruption";
	
	private static final String TAG = "ApplicationModule";
	
	private static final String GCM_SENDER_ID = "gcm.sender_id";
	private static final String REITTIOPAS_BASE_URL = "reittiopas.base_url";
	private static final String REITTIOPAS_USER_ID = "reittiopas.user_id";
	private static final String REITTIOPAS_PASSWORD = "reittiopas.password";
	private static final String HSLPUSH_BASE_URL = "hslpush.base_url";
	
	private static final String ADMOB_PUBLISHER_ID = "admob.publisher_id";
	private static final String ADMOB_TEST_DEVICES = "admob.test_devices";
	
	@Override
	protected void configure() {
		requestStaticInjection(EnvironmentConfig.class);
		requestStaticInjection(GCMIntentService.class);
		requestStaticInjection(MockNotificationReceiver.class);
	}
	
	/** Google Cloud Messaging cannot be used on the default simulator, therefore it needs to be disabled for development */
	@Provides
	public GCMRegistrationService getGCMRegistrationService(Properties environmentConfig) {
		if(isRunningOnSimulator()) {
			return new DevelopmentGCMRegistrationService();
		} else {
			LiveGCMRegistrationService service = new LiveGCMRegistrationService(environmentConfig.getProperty(GCM_SENDER_ID));
			return service;
		}		
	}

	@Provides @Singleton
	public Properties getEnvironmentConfig(Resources resources) {
		Properties properties = new Properties();
		try {
		if(isRunningOnSimulator()) {
			  InputStream rawResource = resources.openRawResource(R.raw.development);
			  properties.load(rawResource);
			  rawResource.close();
		} else {
			  InputStream rawResource = resources.openRawResource(R.raw.live);
			  properties.load(rawResource);
			  rawResource.close();
		}	
		
		} catch(IOException ex) {
			Log.e(TAG, "Reading of application properties failed. You may need to create the appropriate files, see documentation online", ex);
		}
		
		return properties;
	}
		
	@Provides
	@Singleton
	public UserProfileFactory getUserProfileFactory(GCMRegistrationService gcmRegistrationService, Properties environmentConfig, SharedPreferences preferences) {
		return new UserProfileFactory(gcmRegistrationService, environmentConfig.getProperty(HSLPUSH_BASE_URL), preferences);
	}

	@Provides
	@Singleton
	public FindLinesService getFindLinesService(Context ctx, ReittiopasService service) {
		return new FindLinesServiceImpl(service, new LocationUpdateAgent(ctx));
	}
	
	@Provides 
	@Singleton
	public ReittiopasService getReittiopasService(Properties environmentConfig) {
		return new ReittiopasService(
				environmentConfig.getProperty(REITTIOPAS_USER_ID),
				environmentConfig.getProperty(REITTIOPAS_PASSWORD),
				environmentConfig.getProperty(REITTIOPAS_BASE_URL));
	}
	
	@Provides
	public AdViewFactory getAdViewFactory(Context ctx, Properties environmentConfig) {
		String[] testDevices = environmentConfig.getProperty(ADMOB_TEST_DEVICES).split(",");	
		return new AdViewFactory(environmentConfig.getProperty(ADMOB_PUBLISHER_ID), testDevices);
	}
	
	@Provides
	public DisruptionNotifier getDisruptionNotifier(SharedPreferences preferences) {
		return new DisruptionNotifier(preferences);
	}
	
	private boolean isRunningOnSimulator() {
		// this check is not guaranteed to work with all simulators
		return Build.BRAND.startsWith("generic") || Build.DEVICE.startsWith("generic");
	}
	

}
