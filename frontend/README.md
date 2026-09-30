# servdesk UI

The Angular agent workbench ([ADR-0003](../docs/adr/0003-ui-is-an-angular-spa-over-the-public-api.md)),
a client of servdesk's public `/api/**`. `./mvnw package` builds it into the jar; the repository
root's `README.md` and `CLAUDE.md` cover how it fits together.

```bash
npm ci
npm start       # http://localhost:4200, /api proxied to the app on :8080
npm test
npm run lint
```

`src/app/api/servdesk-api.d.ts` is generated from `../src/main/resources/static/openapi/servdesk-api.yaml`
before every start, build, test and lint. Don't edit it; change the contract.

Translations live in `public/i18n/{en,de}.json`. Every user-visible string goes there, in both files.
