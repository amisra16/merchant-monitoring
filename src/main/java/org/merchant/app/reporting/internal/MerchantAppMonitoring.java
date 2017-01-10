package org.merchant.app.reporting.internal;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;

import java.io.File;
import java.io.IOException;


/**
 * Created by awmishra on 1/10/2017.
 */
@Component
public class MerchantAppMonitoring {
    // The directory where the user's credentials will be stored.
    private static final File DATA_STORE_DIR = new File(
            System.getProperty("user.home"), ".store/hello_analytics");

    private static final String APPLICATION_NAME = "Hello Analytics";
    private static final String SERVICE_ACCT_ID = "awanish-test@testing-154814.iam.gserviceaccount.com";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static NetHttpTransport httpTransport;
    private static FileDataStoreFactory dataStoreFactory;

    public static void main(String[] args) {
        SpringApplication.run(MerchantAppMonitoring.class, args);
    }

    @Scheduled(fixedDelay = 5000)
    private static void monitor() {
        try {
            Analytics analytics = initializeAnalytics();
            String profile = getFirstProfileId(analytics);
            printResults(getResults(analytics, profile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Analytics initializeAnalytics() throws Exception {

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);


/*    // Using Client Id and Secret
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, "590045870537-p01b97u4npir953hqmbkkml0qjnp4qto.apps.googleusercontent.com","iPOsSS4WVVmV-73f71JpiaI4",
            Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY)).setDataStoreFactory(
            dataStoreFactory).build();
    // Authorize.
    Credential credential = new AuthorizationCodeInstalledApp(flow,
            new LocalServerReceiver()).authorize("user");*/

        // Using P12 Key

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(SERVICE_ACCT_ID)
                .setServiceAccountPrivateKeyFromP12File(new File("../../../../resources/serviceaccount.p12"))
                .setServiceAccountScopes(AnalyticsScopes.all())
                .setServiceAccountUser("dl-pp-ma-l1-support@paypal.com")
                .build();


        // Construct the Analytics service object.
        return new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
    }

    private static String getFirstProfileId(Analytics analytics) throws IOException {
        // Get the first view (profile) ID for the authorized user.
        String profileId = null;

        // Query for the list of all accounts associated with the service account.
        Accounts accounts = analytics.management().accounts().list().execute();

        if (accounts.getItems().isEmpty()) {
            System.err.println("No accounts found");
        } else {
            String firstAccountId = accounts.getItems().get(0).getId();

            // Query for the list of properties associated with the first account.
            Webproperties properties = analytics.management().webproperties()
                    .list(firstAccountId).execute();

            if (properties.getItems().isEmpty()) {
                System.err.println("No properties found");
            } else {
                String firstWebpropertyId = properties.getItems().get(0).getId();

                // Query for the list views (profiles) associated with the property.
                Profiles profiles = analytics.management().profiles()
                        .list(firstAccountId, firstWebpropertyId).execute();

                if (profiles.getItems().isEmpty()) {
                    System.err.println("No views (profiles) found");
                } else {
                    // Return the first (view) profile associated with the property.
                    profileId = profiles.getItems().get(0).getId();
                }
            }
        }
        return profileId;
    }

    private static GaData getResults(Analytics analytics, String profileId) throws IOException {
        // Query the Core Reporting API for the number of sessions
        // in the past seven days.
        return analytics.data().ga()
                .get("ga:" + profileId, "today", "today", "ga:exceptions")
                .setFilters("ga:dimension3!=401;ga:dimension3!~2[0-9]{2}")
                .execute();
    }

    private static void printResults(GaData results) {
        // Parse the response from the Core Reporting API for
        // the profile name and number of sessions.
        if (results != null && !results.getRows().isEmpty()) {
            System.out.println("View (Profile) Name: "
                    + results.getProfileInfo().getProfileName());
            Integer exceptions = Integer.valueOf(results.getRows().get(0).get(0));
//      Integer hits = Integer.valueOf(results.getRows().get(0).get(1));
            System.out.println("Total Exceptions: " + exceptions.toString());
//      System.out.println("Total Hits: " + hits.toString());
//      System.out.println("API Failure % " + (exceptions/hits)*100 +" %");
        } else {
            System.out.println("No results found");
        }
    }
}

