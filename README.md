# README

## Requirements

- **Java**: 21
- **Server**: Must have Chrome installed
- **Firebase**: Create a secret with the name `google-services.json` and place it under `src/main/resources/google-services.json`
- **Database Configuration**: Set up the database URL in the `application.yml` file.

## Example Usage

# Step Actions Guide

This document provides a guide for interacting with specific step actions in the app.

## Process Steps

1. **APP_STARTED**
    - When the app starts for the first time, it will register a `processStep` called `APP_STARTED` to indicate that the app has started successfully.

2. **CONNECT_GOOGLE**
    - To launch the browser, update the `processStep` to `CONNECT_GOOGLE`.

3. **CONNECTED_LOGIN_FORM**
    - When the app successfully connects to Google and opens the Google login form, the `processStep` will be updated to `CONNECTED_LOGIN_FORM`.

4. **LOGIN_SUCCESS**
    - After the user logs in successfully, the `processStep` will change to `LOGIN_SUCCESS`.

5. **LOGIN_FAILURE**
    - If the user does not log in successfully within 5 minutes, the `processStep` will update to `LOGIN_FAILURE`.

6. **UPDATED_THE_PROFILE_FOLDER**
    - After a successful login and renaming the profile folder to the user's Google email, the app will change to the `UPDATED_THE_PROFILE_FOLDER` state.

## Status List

- `APP_STARTED`
- `ERROR`
- `CONNECT_GOOGLE`
- `CONNECTED_LOGIN_FORM`
- `CAN_NOT_FIND_LOGIN_FORM`
- `LOGIN_SUCCESS`
- `LOGIN_FAILURE`
- `UPDATED_THE_PROFILE_FOLDER`

## Notes

- When the app is in the states `CONNECT_GOOGLE`, `CONNECTED_LOGIN_FORM`, or `LOGIN_SUCCESS`, you should not update the status further.
- Otherwise, you can update to `CONNECT_GOOGLE` to restart the process.


