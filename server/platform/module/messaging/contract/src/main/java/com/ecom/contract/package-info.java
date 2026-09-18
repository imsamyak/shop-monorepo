/**
 * Messaging contracts, event schemas, and payload specifications.
 * <p>
 * Every event carries the {@code version} of its aggregate after the change. The producer takes it from the entity
 * it just saved, so versions of one aggregate only grow. A consumer that builds a view from events keeps the highest
 * version it has applied per aggregate and ignores anything not newer, which makes duplicates and out-of-order delivery
 * harmless. Events that remove an aggregate ({@code Deleted}) are final and are applied whatever their version.
 */
package com.ecom.contract;
