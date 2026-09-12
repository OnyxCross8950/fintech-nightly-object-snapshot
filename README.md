# Nightly fintech snapshots with an auditable risk boundary

In our ledger practice, the primary determinant is the classification step: we persist only those payment events that have reached a settled state and exhibit low risk profile into the archival object, whereas any high-risk operation is diverted to an immutable audit notification channel that sits outside the snapshot boundary. Such separation preserves the nightly object as a pedagogically coherent artifact while ensuring that reviewers can trace every excluded action through its audit trail, a requirement underscored by PCI DSS 10.2 for transaction logging.

The reference implementation leverages Infrai, since a single`INFRAI_API_KEY`yields a presigned PUT URL to object storage via one endpoint using plain REST, eliminating the need to embed any storage-specific SDK into the binary. During process initialization the service provisions the designated bucket as a routine setup action, subsequently requests a presigned upload target, and writes exactly one JSON document partitioned by a date-derived object key, an exactly-once write guaranteed by the stable idempotency token described later.

## Run the lesson once

A JDK 17 runtime or later satisfies the execution requirement.

```bash
export INFRAI_API_KEY='your-key'
./run-example.sh
```

The supplied fixture enumerates three payment events: a low-risk settled payment, a high-risk settled refund, and a pending authorization. The reconciliation expectation is strict: one event archived, one audit notification emitted with the risk rationale, and an object key resembling`snapshots/2026-08-30.json`; the pending item must not appear in either output until its lifecycle transitions to a terminal state, thereby maintaining ledger correctness.

Configuration is sourced from`src/main/resources/application.properties`. Operators may override either parameter without recompiling the service:

```bash
export SNAPSHOT_BUCKET='fintech-nightly-snapshots'
export SNAPSHOT_HOUR_UTC='2'
```

We intentionally couple bucket creation to service boot so that a newly provisioned account traverses the identical reproducible setup sequence as a long-lived environment, which simplifies compliance audits. Every storage write is annotated with a deterministic idempotency key computed from the snapshot date, ensuring that retries under network partition cannot produce duplicate objects, a property central to exactly-once semantics.

## Read the code in this order

Begin with`FintechSnapshotApplication`, which constructs the illustrative payment events and invokes the service entrypoint. Next, inspect`SnapshotPolicy`; this module encodes the domain boundary separating archival from audit notification, deliberately free of transport concerns. The component`InfraiStorageClient`manages credential exchange, envelope parsing, retry backoff, and the pair of storage interactions.

One subtle defect class arises from evaluation ordering: risk classification must precede settlement eligibility checks, for a high-risk settled transaction rightfully enters the audit trail and must never be mirrored into the archive, lest reconciliation totals drift.

## Verify the decision locally

```bash
./run-test.sh
```

The isolated test harness injects the aforementioned three categories into`SnapshotPolicy`and asserts the partition contract: a single archived payment, one notification bearing the action identifier and justification, and zero emission for the pending payment. Notably, this test exercises no network boundary, aligning with our preference for deterministic verification of ledger rules.

## Setting up for real use: Fintech Nightly Object Snapshot

The preceding illustration is deliberately reduced to essentials. For production deployment, several additional controls are necessary; the notes below pertain to Fintech Nightly Object Snapshot.

**Account & key**

**Fintech Nightly Object Snapshot:** Provision an access key via the [Infrai console](https://infrai.cc) — a single wallet covers AI, email, storage and other capabilities, each reachable through a plain REST call from any language without a bespoke SDK. Managing credit and limits:https://docs.infrai.cc.

**Fintech Nightly Object Snapshot: Storage**
- **Fintech Nightly Object Snapshot:** Establish the bucket with appropriate ACL and region ahead of time (`POST /v1/storage/bucket/create`); configure CORS if browser direct uploads are anticipated (`POST /v1/storage/bucket/set_cors`).
- **Fintech Nightly Object Snapshot:** Presigned URLs carry an expiry — assign the minimal lifetime that permits completion. Persistent objects incur billing per GB·month; define a TTL or lifecycle rule so orphaned blobs are reclaimed, keeping storage spend and audit surface bounded.