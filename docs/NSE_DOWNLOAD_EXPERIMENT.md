# NSE EOD downloader — technical reproduction

**Status:** Experimental, manually requested implementation. Not an approved production acquisition route.

## What was reproduced

The downloader in the separate `NIFTY_Seller_Intelligence_project` repository succeeded on a GitHub-hosted Ubuntu runner by using Python Requests against the official NSE archive file URLs. The important difference from the earlier Support-Resistance smoke test was the fallback sequence across three hostnames and up to three attempts per hostname:

1. `archives.nseindia.com`
2. `nsearchives.nseindia.com`
3. `www.nseindia.com`

This implementation follows that technique. It requests three source families for explicitly supplied sessions:

- UDiFF F&O Common Bhavcopy ZIP.
- Daily F&O MII contract-master GZIP.
- NIFTY 50 daily index OHLC CSV.

It uses a Linux Chrome User-Agent, a Referer to the official NSE site, Requests streaming in 1 MiB chunks, a 20/90-second connect/read timeout, three attempts per host, and SHA-256 while writing a temporary file. It does not use an NSE API, Extranet credentials, cookies, browser automation, or a reverse-engineered JSON endpoint.

## Validation and output

- Bhavcopy: ZIP integrity, exactly one CSV member and a non-empty CSV header.
- MII: GZIP decompression and a non-empty CSV header.
- NIFTY 50: required index OHLC columns, NIFTY 50 row and matching index date.
- Each file record stores the source URL, final URL, HTTP status, byte count, SHA-256, download timestamp, filename and validation result.
- Failed attempts are retained in `artifacts/nse-eod/manifest.json`; a missing source date is not substituted with another date.
- Original downloaded bytes are kept unchanged in the artifact. Raw files are not committed to Git and are not uploaded to B2.
- GitHub artifact retention is set to one day. This is not the project's five-hour cleanup implementation.

## Trigger and limits

The workflow has no schedule. It supports manual `workflow_dispatch`; for this first reproduction only, a push commit whose message contains `[nse-download-once]` triggers the initial test while the work remains on the feature branch. Ordinary pushes skip the download job.

**Technical success is not permission.** NSE Terms of Use prohibit systematic/automated data collection on or in relation to its website. A manually dispatched GitHub Action still automates HTTP retrieval and may fall within that restriction. This experiment was added only because the project owner explicitly requested a technical reproduction; it does not change the project's approved source-use policy, establish a licence, approve repeated collection, or authorise persistent storage, ML training, trading/signal use, or dashboard redistribution. Do not schedule or repeatedly run it. Obtain written NSE confirmation first; until then, treat any artifact only as a short-lived technical test and do not use its contents for analysis or model training.

## Current Support-Resistance validation state

- Implementation commit: `cf8efb82226fb825a799faec4d98e4cd75b3f675` on `feature/nifty-seller-intelligence-foundation`.
- GitHub Actions regression run [#114](https://github.com/naveenm2020it-glitch/Support-Resistance/actions/runs/45806264745) passed with 54 tests.
- The acquisition workflow itself has **not** run in Support-Resistance. Therefore no successful download, report count, checksum manifest or artifact is claimed for this repository.
- The workflow file currently exists on the feature branch only. GitHub's manual workflow dispatch becomes available from the Actions UI after the workflow is present on the repository's default branch. The existing draft PR has not been merged; `main` remains unchanged.
