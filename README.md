
# CDS File Upload Frontend

This is a placeholder README.md for a new repository

## Setup

In order to run CDS file upload frontend locally you will need to be running service manager and run the command

```bash
sm --start CDS_FUF_ALL -f
```

#### Seeding Data in `api-subscription-fields`

`cds-file-upload-frontend` requires data to be seeded in `api-subscription-fields` before a full journey can
be completed. The following command will seed the correct information

```bash
curl -v -X PUT "http://localhost:9650/field/application/cds-file-upload-frontend/context/customs%2Fdeclarations/version/3.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "https://postman-echo.com/post", "securityToken" : "securityToken" } }'
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
