# AdminPortal

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.2.6.

## Development server

To start a local development server, run:

```bash
ng serve
```

API calls use relative `/ldms-*` URLs on the dev server (`http://localhost:4200`); `proxy.conf.json` forwards them to the **API Gateway** (`http://localhost:8091`) so the browser does not hit CORS.

### Local backend startup order (sign-in)

| Order | Service | Port | Required for login |
|------|---------|------|-------------------|
| 1 | MySQL | 3306 | Yes |
| 2 | ldms-user-management | 8086 | Yes (user + roles) |
| 3 | ldms-authentication | **8083** | Yes (issues JWT) |
| 4 | ldms-api-gateway | **8091** | Yes (proxied from :4200) |
| 5 | ng serve (admin portal) | 4200 | UI only |

Login endpoint (via dev proxy → gateway): `POST http://localhost:4200/ldms-authentication/v1/auth/request-access-token`

If the gateway logs `Connection refused: /127.0.0.1:8083`, **ldms-authentication is not running** (or the gateway is pointed at the wrong port) — start auth in IntelliJ, restart the gateway, then retry login.

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
