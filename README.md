# cds-file-upload-frontend

## About
This public-facing microservice is part of the Customs Declaration Service (CDS). It is designed to work in tandem with the [cds-file-upload](https://github.com/hmrc/cds-file-upload) back-end service.

It lets traders upload supporting documents for their import or export declarations, and read and reply to their CDS secure messages.

Once a file is submitted it is scanned by Upscan. The [Notification Service](https://github.com/hmrc/customs-notification) sends the result of the scan to a callback URL, and the result is persisted. After all files are submitted, the service checks that every file has a success notification before showing the final File Receipt page.

See the [Secure File Upload UI Service - Solution Design](https://confluence.tools.tax.service.gov.uk/display/CD/Secure+File+Upload+UI+Service+-+Solution+Design) for more details.

| | |
|---|---|
| Digital service | CDS Exports |
| Local port | `6793` |
| Base path | `/cds-file-upload-service` |
| Back-end | [cds-file-upload](https://github.com/hmrc/cds-file-upload) (port `6795`) |
| Acceptance tests | [sfus-acceptance-ui-journey-tests](https://github.com/hmrc/sfus-acceptance-ui-journey-tests) |
| Performance tests | [cds-file-upload-performance-tests](https://github.com/hmrc/cds-file-upload-performance-tests) |

## How to Run this Service

### Prerequisites
Start all the services this service depends on with [Service Manager](#service-manager-profiles):

```bash
./run-services.sh
```

This runs `sm2 --start CDS_FUF_ALL`.

### Running the service locally
To run this service from source (for example, to test your own changes), stop the instance started by Service Manager and run it with the stubbed endpoints:

```bash
sm2 --stop CDS_FILE_UPLOAD_FRONTEND
./run-with-stubs.sh
```

`run-with-stubs.sh` runs `sbt run -Dconfig.resource=test.conf`. `test.conf` enables the test-only routes, which contain the [fake S3 endpoint](#how-the-downstream-services-are-faked) needed to upload files locally. If you run sbt yourself, you must enable the test-only routes:

```bash
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

The service starts on port `6793`. To access it, [sign in through the auth login stub](#enrolment-required) and you will be redirected to http://localhost:6793/cds-file-upload-service/start.

### How the downstream services are faked
We use the [customs-declarations-stub](https://github.com/hmrc/customs-declarations-stub) `/file-upload` endpoint to stub out the call this service makes to the [customs-declarations](https://github.com/hmrc/customs-declarations) API to fetch the list of S3 bucket URLs to upload to. The stub returns fake S3 URLs that point to the test-only endpoint `/cds-file-upload-service/test-only/s3-bucket` on this service.

So if you're running this service locally or in an environment that uses stubs, you must start this service with the test-only routes enabled (see [Running the service locally](#running-the-service-locally)), because the test routes contain the fake S3 endpoint (that stubs the Upscan service used in production-like environments when users upload files from their browser).

### Testing the file upload feature
In the QA and Production environments, when a user submits a file to upload from their browser, they are actually submitting to an AWS S3 bucket. In the other 'stubbed' environments there is no S3 bucket to upload to, so we have a test-only endpoint on this frontend service that substitutes for the missing S3 URL.

When the test-only endpoint is called, it sends a success notification to our back-end service (imitating what Upscan would normally send) for any file chosen by the user. You can however also mimic an Upscan **rejection** notification by uploading a file whose filename begins with an `x`.

## How to Test this Service

### Local

#### Unit and integration tests
```bash
sbt test it/test
```

#### Pre-push check
There is a script called `precheck.sh` that runs all unit and integration tests, examines their coverage (minimum 90%) and checks that all files are properly formatted.
It is good practice to run it just before pushing to GitHub.

```bash
./precheck.sh
```

#### Acceptance tests
Once your changes are done, run the [sfus-acceptance-ui-journey-tests](https://github.com/hmrc/sfus-acceptance-ui-journey-tests) against your locally running copy of this service (see [Running the service locally](#running-the-service-locally)).

For the Service Manager profile and the script options, see that repository's README.

#### Performance tests
The performance tests for this service are in [cds-file-upload-performance-tests](https://github.com/hmrc/cds-file-upload-performance-tests). For how to run them and which environments they run against, see that repository's README.

### Staging
To check the service manually, sign in through the auth login stub at https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in, using the [enrolment details](#enrolment-required) below and the redirect URL https://www.staging.tax.service.gov.uk/cds-file-upload-service/start.

### QA
QA uses real upstream services (including Upscan and S3), so files are scanned for real and the test-only S3 endpoint is not available.

## Service Catalogue
- [cds-file-upload-frontend in the MDTP Catalogue](https://catalogue.tax.service.gov.uk/repositories/cds-file-upload-frontend)

## Jenkins Pipeline
- [cds-file-upload-frontend build](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSExports/job/cds-file-upload-frontend/)

The acceptance test Jenkins jobs are listed in the [sfus-acceptance-ui-journey-tests README](https://github.com/hmrc/sfus-acceptance-ui-journey-tests).

## Smoke and Regression Test Coverage
The smoke and regression tests for this service are in [sfus-acceptance-ui-journey-tests](https://github.com/hmrc/sfus-acceptance-ui-journey-tests). They cover the file upload journey (MRN entry, contact details, number of files, upload and receipt) and the secure messaging journeys.

For how to run each test pack and the full list of scenarios, see that repository's README.

## Service Manager Profiles
These profiles are defined in [service-manager-config](https://github.com/hmrc/service-manager-config).

| Profile | Use |
|---|---|
| `CDS_FUF_ALL` | Running and developing this service locally (started by `./run-services.sh`) |

| Service | Port | Repository |
|---|---|---|
| `CDS_FILE_UPLOAD_FRONTEND` | 6793 | This service |
| `CDS_FILE_UPLOAD` | 6795 | [cds-file-upload](https://github.com/hmrc/cds-file-upload) |
| `CUSTOMS_DECLARATIONS_STUB` | 6790 | [customs-declarations-stub](https://github.com/hmrc/customs-declarations-stub) (also stubs secure messaging locally) |

### Accessibility Statement
As a developer we rarely have the need to test it locally, therefore we should not add the
[Accessibility Statement Frontend](https://github.com/hmrc/accessibility-statement-frontend) microservice to our service
profile, as it will consume more memory and CPU.

If we need to test and/or update our accessibility statement then ensure the current service profile has started and
run the following:

```bash
sm2 --start ACCESSIBILITY_STATEMENT_FRONTEND
```

It is then available at http://localhost:12346/accessibility-statement/cds-file-upload.

## Endpoints

All paths are relative to `/cds-file-upload-service` and, unless stated otherwise, need an [authenticated user with the HMRC-CUS-ORG enrolment](#enrolment-required).

| Method | Path | Purpose | Sample request (query / form body) | Response |
|---|---|---|---|---|
| GET | `/` or `/start` | Service start | – | `303` → `/what-do-you-want-to-do` |
| GET | `/what-do-you-want-to-do` | Choose between uploading documents and reading messages | – | `200` |
| POST | `/what-do-you-want-to-do` | Submit the choice | `choice=DocumentUpload` (or `MessageInbox`) | `303` → `/mrn-entry`, or `/message-choice` |
| GET | `/mrn-entry` | Enter the MRN of the declaration | – | `200` |
| POST | `/mrn-entry` | Submit the MRN | `value=24GB1J1V8TS3JU1AR8` | `303` → `/contact-details`<br>Invalid or not allowed: `400` |
| GET | `/mrn-entry/:mrn` | Pre-fill the MRN (used by [customs-declare-exports-frontend](https://github.com/hmrc/customs-declare-exports-frontend)) | `/mrn-entry/24GB1J1V8TS3JU1AR8` | `303` → `/contact-details` |
| GET / POST | `/contact-details` | Contact details for the upload | `name=Joe+Bloggs&companyName=Acme&phoneNumber=01234567890` | `303` → `/how-many-files-upload` |
| GET / POST | `/how-many-files-upload` | Number of files to upload (1 to 10) | `value=2` | `303` → `/upload/:ref` |
| GET | `/upload/:ref` | Upload one file | – | `200`<br>Already uploaded: `303` → next file or `/upload/receipt` |
| GET | `/upload/upscan-success/:id` | Upscan redirect after a successful upload | – | `303` → next file, `/upload/receipt` or `/upload-error` |
| GET | `/upload/upscan-error/:id` | Upscan redirect after a failed upload | – | `200` |
| GET | `/upload/receipt` | File Receipt page | – | `200` |
| GET / POST | `/message-choice` | Choose exports or imports messages | `choice=ExportMessages` (or `ImportMessages`) | `303` → `/messages` |
| GET | `/exports-message-choice` | Go straight to exports messages | – | `303` → `/messages` |
| GET | `/messages` | Secure messages inbox | – | `200` |
| GET / POST | `/conversation/:client/:conversationId` | View or reply to a conversation | – | `200` / `303` → `/conversation/:client/:conversationId/result` |
| GET | `/language/:lang` | Switch language. *No authentication needed* | `english` or `cymraeg` | `303` |
| GET | `/sign-out` | Sign out | `?signOutReason=UserAction` (or `SessionTimeout`) | `303` → `bas-gateway` sign-out |

Test-only (enabled with `testOnlyDoNotUseInAppConf.Routes`):

| Method | Path | Purpose |
|---|---|---|
| POST | `/cds-file-upload-service/test-only/s3-bucket` | Fake S3 upload endpoint; sends a success notification (or a rejection for files starting with `x`) |

## Enrolment Required

Locally, sign in through the auth login stub at http://localhost:9949/auth-login-stub/gg-sign-in, enter the values below, and submit.

| Field | Value |
|---|---|
| Redirect URL | Local: `http://localhost:6793/cds-file-upload-service/start`<br>Staging: `https://www.staging.tax.service.gov.uk/cds-file-upload-service/start` |
| Enrolment Key | `HMRC-CUS-ORG` |
| Identifier Name | `EORINumber` |
| Identifier Value | A GB EORI number, e.g. `GB123456789006` |

The user must also have a verified email address; otherwise they are redirected to `/unverified-email`.

## Developer Notes

### Config settings
The following settings are used to configure the notifications persistence and retrieval:

```
notifications {
  max-retries = 1000
  retry-pause-millis = 250
}
```

- `max-retries`: after an initial attempt to retrieve all notifications, this is the maximum number of retries to attempt before failing with an error.
- `retry-pause-millis`: this is the number of milliseconds to wait between retries.

The following settings determine how long the user's answers are persisted before automatic deletion:

```
file-upload-answers-repository {
  ttl-seconds = 3600
}

secure-message-answers-repository {
  ttl-seconds = 3600
}
```

- `ttl-seconds`: this is the Time To Live, in seconds. It cannot be changed after the initial deployment without manually dropping the index.

The accepted files are configured under `file-formats` (maximum size 10MB; `.pdf`, `.png`, `.jpg`, `.jpeg`, `.txt`, `.doc`, `.docx`, `.csv`, `.xls`, `.xlsx`, `.ods`, `.odt`).

### Scalafmt
The code is formatted with [sbt-scalafmt](https://scalameta.org/scalafmt/docs/installation.html#sbt), using the rules in `.scalafmt.conf`.

Check that all project files are formatted as expected:

```bash
sbt scalafmtCheckAll scalafmtSbtCheck
```

Format `*.sbt` and `project/*.scala` files:

```bash
sbt scalafmtSbt
```

Format all project files:

```bash
sbt scalafmtAll
```

## License
This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
