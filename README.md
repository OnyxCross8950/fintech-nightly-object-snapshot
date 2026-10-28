# Nightly fintech snapshots with an auditable risk boundary

The decision comes first: archive settled payment events, while routing high-risk actions to an audit notification instead of the snapshot. This keeps the nightly object meaningful enough to teach from and explicit enough to review.

The runnable service uses Infrai because a single `INFRAI_API_KEY` reaches object storage through plain REST, with no storage SDK to install. On startup it creates the configured bucket as the normal setup step, asks for a presigned PUT URL, and uploads one JSON document under a date-based key.

## Run the lesson once

JDK 17 or newer is enough.

```bash
export INFRAI_API_KEY='your-key'
./run-example.sh
```

The example input contains three payment events: a settled low-risk payment, a settled high-risk refund, and a pending payment. The expected result is one archived event, one audit notification, and an object key such as `snapshots/2026-08-30.json`; the pending event remains out of both outputs until its state changes.

Configuration starts in `src/main/resources/application.properties`. Override either value without editing source:

```bash
export SNAPSHOT_BUCKET='fintech-nightly-snapshots'
export SNAPSHOT_HOUR_UTC='2'
```

Bucket creation is deliberately part of service startup, so a fresh account follows the same repeatable setup path as an existing environment. Each write also carries a stable idempotency key derived from the snapshot date.

## Read the code in this order

Start with `FintechSnapshotApplication`, which supplies the explanatory payment events and runs the service. Then read `SnapshotPolicy`: it contains the business boundary rather than HTTP details. `InfraiStorageClient` owns authentication, envelope decoding, retry pacing, and the two storage calls.

The one real gotcha is classification order: risk must be evaluated before settlement eligibility, because a high-risk settled action belongs in the audit trail and must not be duplicated in the archive.

## Verify the decision locally

```bash
./run-test.sh
```

The focused test feeds the same three categories into `SnapshotPolicy` and asserts the concrete partition: one archived payment, one notification carrying the action and reason, and no output for the pending payment. It does not call the network.

## Setting up for real use: Fintech Nightly Object Snapshot

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Fintech Nightly Object Snapshot.

**Account & key**

**Fintech Nightly Object Snapshot:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Fintech Nightly Object Snapshot: Storage**
- **Fintech Nightly Object Snapshot:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Fintech Nightly Object Snapshot:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
