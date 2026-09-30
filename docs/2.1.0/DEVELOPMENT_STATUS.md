# V2.1 porting backlog

The final release scope and evidence are recorded in [VALIDATION.md](VALIDATION.md).

After reviewing the compatibility trade-off, the release scope was narrowed to the profiles whose generated Java was actually compiled. This does not mark the complete original 1.17.1–26.2 request as implemented.

Deferred work:

- Additional 1.17–1.19 generators and remaining 1.20/1.21/26.x loader/version combinations.
- Transport adapters and real builds for the excluded experimental 1.20.x profiles.
- Interactive in-game, multiplayer and dedicated-server launch smoke tests.
- Further translations of the new writing interface and migration tooling for changed writing-zone schemas.

Older MCreator support requires a real source/API port, not just changing the plugin manifest. The 2022.2 bridges live in the plugin's own namespace, and all build adapters modify staging copies only.
