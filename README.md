
# CDS File Upload Frontend

This app allows users to upload files to support their import or export declaration. Once a file is submitted it will be scanned. 
The [Notification Service](https://github.com/hmrc/customs-notification) will send a notification about the result of this scan via a callback URL. This is persisted by the app. After all files are submitted, the app will check that all files have success notifications before proceeding to the final File Receipt page.

See [here](https://confluence.tools.tax.service.gov.uk/display/CD/Secure+File+Upload+UI+Service+-+Solution+Design) for more details.
 

## Running the app locally

The app simulates the Upscan service locally using a stub.


In order to run CDS File Upload Frontend with the stubbed endpoints you will need to be running service manager and run the following commands

```
./run-services.sh

./run-with-stubs.sh
```

If you're running service manually without using the bash scripts on localhost you must use `sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes` because test routes contains endpoints that are treated like a stub for uploading files.

Inside `application.conf` you can find that `customs-declarations` settings not point to the different repo but to the endpoints that are in the test routes.

### Config settings

The following settings are used to configure the notifications persistance and retrieval.

max-retries: After an initial attempt to retrieve all notifications, this is the maximum number of retries to attempt before failing with an error.

retry-pause-millis: This is the number of milliseconds to wait between retries.

auth-token: This is the Authorisation Token we expect from the Notification Service.

ttl-seconds: This is the Time To Live for the notification.  This is a value in seconds that determines how long the notification will be persisted before automatic deletion.
```
notifications {
  max-retries = 10
  retry-pause-millis = 500
  auth-token = "Basic NDg5MDc0YjAtOWZmZi00NjY5LTk0NTktNGE0YjM5YTk3NDU5IDpCcSVTOjZKQktTS1Y2UGI"
  ttl-seconds = 3600
}
```

### Feature flags
To set a feature flag via system properties

`sbt "run -Dmicroservice.services.features.secureMessaging=enabled"`

### Accessibility Statement

As a developer we rarely have the need to test it locally, therefore we should not add the 
[Accessibility Statement Frontend](https://github.com/hmrc/accessibility-statement-frontend) microservice to our service
profile, as it will consume more memory and CPU.

If we need to test and/or update our accessibility statement then ensure the current service profile has started and 
run the following:

```
# Start the microservice
sm --start ACCESSIBILITY_STATEMENT_FRONTEND
```

It is then accessible [here](http://localhost:12346/accessibility-statement/cds-file-upload). 

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
