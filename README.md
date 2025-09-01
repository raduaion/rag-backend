
## Build the docker image
docker build . -t hermes-backend:latest

## Update config
- see <YOUR_PATH_TO_CONFIG>, <YOUR_PATH_TO_LOGS> and <YOUR_PATH_TO_GOOGLE_CREDENTIALS_JSON> in docker-compose.json
- create deployment.json and secrets.json in <YOUR_PATH_TO_CONFIG> based on their respective .template file in src/main/resources

## Run it
docker compose up backend

## Third-party service to be implemeted

# Authentication service
  Machine-to-Machine (M2M) Authentication using OAuth2 Client Credentials Flow

  This document outlines the procedure for a backend service (a "client") to authenticate itself with the authorization server and obtain an access token for secure API communication. This
  process uses the OAuth2 client_credentials grant type, which is designed for server-to-server interactions where a direct user is not involved.

  Step 1: Obtain an Access Token

  To get an access token, the client must send a POST request to the token endpoint.

   * Endpoint: /oauth/token
   * Method: POST
   * Headers:
       * Authorization: Basic <credentials>
           * The <credentials> value is the Base64-encoded string of the client's client_id and client_secret joined by a colon (:). For example, Base64(your_client_id:your_client_secret).
       * Content-Type: application/x-www-form-urlencoded
   * Body (Form URL Encoded):
       * grant_type: client_credentials
       * scope: A space-separated list of requested permissions (e.g., read write).

  Example Request using cURL:
  ```
   curl -X POST "https://your-auth-server.com/oauth/token" \
        -H "Authorization: Basic $(echo -n 'your_client_id:your_client_secret' | base64)" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&scope=read"
  ```

  Step 2: Handle the Response

  A successful request will return a JSON object containing the access token and its metadata.

  Example Success Response (200 OK):
  ```
   {
       "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
       "token_type": "bearer",
       "expires_in": 43199,
       "scope": "read"
   }
  ```

  The client application should securely store the access_token and calculate its expiration time. It is recommended to subtract a small buffer (e.g., 15-30 seconds) from the expires_in
  value to account for network latency.

   * `access_token`: The token used to authenticate subsequent API requests.
   * `expires_in`: The lifetime of the access token in seconds.

  Step 3: Use the Access Token

  To make authenticated requests to a resource server, include the obtained access_token in the Authorization header with the Bearer scheme.

  Example API Request using cURL:
  ```
   curl -X GET "https://your-api-server.com/resource" \
        -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  ```

  Step 4: Manage Token Expiration

  The access token is short-lived. The client must track its expiration. Before making an API call, check if the current token is expired. If it is, the client must request a new one by
  repeating Step 1.

  The client_credentials flow does not use refresh tokens; the client simply re-authenticates with its ID and secret to get a new access token.


# Email service
API: /send-immediately

  This endpoint synchronously accepts and attempts to send a single email. The request must be sent with a Content-Type of multipart/form-data.

  ---

  Endpoint

  POST /send-immediately

  Authentication

  The API is secured using OAuth2 and expects a JSON Web Token (JWT) to be passed as a bearer token in the Authorization header of the request.

   * Header: Authorization
   * Value: Bearer <YOUR_JWT_ACCESS_TOKEN>


  Request Parameters

  The request must be sent as multipart/form-data with the following fields:

   * sender (string, required): The sender's email address.
   * senderName (string, optional): The sender's display name.
   * recipients (list of strings, required): A list of recipient email addresses. You can provide multiple values for this key.
   * subject (string, required): The subject line of the email.
   * content (string, required): The body of the email. The content is treated as a single block of text (likely HTML, but the API does not distinguish).
   * attachments (list of files, optional): A list of files to attach to the email.

  Example cURL Request:
  ```
   curl -X POST <ENDPOINT>/send-immediately \
     -F "sender=sender@example.com" \
     -F "senderName=Sender Name" \
     -F "recipients=recipient1@example.com" \
     -F "recipients=recipient2@example.com" \
     -F "subject=Your Subject" \
     -F "content=<h1>Hello!</h1><p>This is the email content.</p>" \
     -F "attachments=@/path/to/your/file1.pdf" \
     -F "attachments=@/path/to/your/file2.png"
   ```

  ---

  Responses

  Success Response (`200 OK`)

  If the API accepts the request for delivery, it should return a JSON object containing the message ID.
  ```
   {
     "status": "SUCCESS",
     "data": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
     "error": null
   }
  ```
   * data (string): A unique identifier for this specific email transaction.

  Error Responses

   * `400 Bad Request`: Returned if there is an issue with the provided email addresses (e.g., EmailAddressException).
   * `500 Internal Server Error`: Returned for any other general Exception that occurs during processing.
