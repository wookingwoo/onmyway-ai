# Actual navigation-handoff screenshots

All native frames preserve the complete Pleos left vehicle panel and bottom bar.
Saved PNG pixels match the local original capture; no map/route/UI is fabricated.

| File | Actual scope | SHA256 |
| --- | --- | --- |
| [native-navigation.png](native-navigation.png) | Actual explicit recovery-button SDK screen dispatch. Native map tiles blank; HTTP401 observed; no route success claim. | `ce41e27e976ac5992195044c1fbad43b2159f1392e6e00644c1104a91a82ec3b` |
| [interrupted-guidance.png](interrupted-guidance.png) | Actual app recovery UI from an explicitly seeded hint on an independently selected sample; no real accepted route. | `5831c44f94f27473a552473ed70c7be82979a34a4dd65215c6d529b92e8faac1` |
| [sample-map-blocked.png](sample-map-blocked.png) | Actual selected synthetic flow; map action disabled and no native SDK screen request. | `d0e33c68e309f1bd9b25170b8f5cfdf3f48958dc28e328a220b6c638cde76b8a` |
| [sample-home-arrival.png](sample-home-arrival.png) | Actual selected sample flow, separately approved simulated home after two explicit mandatory completions. | `21f4bbef898487514089617df31e9bfb518453b92b092be794f10af926c7b8bf` |

Actual tile rendering, live planned-route acceptance/arrival and physical-vehicle usability remain unverified. SDK dispatch and native foreground are separate observations. Large APKs/raw/XML/reports stay local. See [handoff behavior](../../navigation-handoff.md).
