# README

## Requirements

- **Java**: 21
- **Server**: Must have Chrome installed
- **Firebase**: Create a secret with the name `google-services.json` and place it under `src/main/resources/google-services.json`
- **Database Configuration**: Set up the database URL in the `application.yml` file.

## Example Usage

To test by adding a task, use the following `curl` command:

```bash
curl 'http://localhost:7171/browser-task/save' \
--header 'Content-Type: application/json' \
--data-raw '{
    "email": "tyler.truong22@gmail.com",
    "status": "NEW"
}'
