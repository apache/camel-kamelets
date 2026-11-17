# Demo Kamelets

These Kamelets read toy data from public services. They are good for a first try of Kamelets, for a demo, or as an
example of how to write a Kamelet. They are not part of the Kamelet catalog: they are not in the
`camel-kamelets` JAR, and they have no page on the catalog website.

| Kamelet | What it does |
|---|---|
| `bitcoin-source` | Reads the current price of Bitcoin in USDT from the public market data of Binance at a regular interval. It does not connect to a Bitcoin node or wallet. |
| `chuck-norris-source` | Fetches a random Chuck Norris joke from api.chucknorris.io at a regular interval. |
| `earthquake-source` | Sends a message for each earthquake reported by the USGS. The developer guide builds it step by step. |
| `wttrin-source` | Fetches the current weather or the forecast for a place from wttr.in at a regular interval. |

They all need access to the internet, and no account or key.

## Run one

Point Camel JBang at this directory with `--local-kamelet-dir`. For example, save this route as `jokes.yaml`:

```yaml
- route:
    from:
      uri: "kamelet:chuck-norris-source"
      parameters:
        period: 10000
      steps:
        - to:
            uri: "log:joke"
```

Then run it from the root of the repository:

```
camel run jokes.yaml --local-kamelet-dir=examples/kamelets
```

Each Kamelet describes its options and its output in `spec.definition`.
