/*
 * Copyright © 2016 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangpushserver.subscription;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf._5277.netconf.rev160615.Stream;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.event.notifications.rev160615.Encodings;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.event.notifications.rev160615.Subscriptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.event.notifications.rev160615.subscriptions.Subscription;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionEngine {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriptionEngine.class);

	// Namespaces for different Moduls
	public static final String YP_NS = "urn:ietf:params:xml:ns:yang:ietf-yang-push";
	public static final String YP_NS_DATE = "2016-06-15";
	public static final String NOTIF_BIS = "urn:ietf:params:xml:ns:yang:ietf-event-notifications";
	public static final String NOTIF_BIS_DATE = "2016-06-15";

	// global data broker
	private DOMDataBroker globalDomDataBroker = null;
	// self instance
	private static SubscriptionEngine instance = null;
	// Subscription ID sid
	private static int sub_id = -1;
	// map of subscriptions
	private Map<String, SubscriptionInfo> masterSubMap = null;

	/**
	 * The selected operation is used to update a subscription stored in MD-SAL
	 *
	 */
	public static enum operations {
		establish, delete, modify,
	}

	/**
	 * Creating protected constructor for creating singleton instance
	 */
	protected SubscriptionEngine() {
		super();
		masterSubMap = new HashMap<String, SubscriptionInfo>();
	}

	/**
	 * getInstance method implements subscription engine as singleton
	 * 
	 * @return this
	 */
	public static SubscriptionEngine getInstance() {
		if (instance == null) {
			instance = new SubscriptionEngine();
		}
		return instance;
	}

	/**
	 * Set global BI data broker to subscription engine
	 * 
	 * @param globalDomDataBroker
	 */
	public void setDataBroker(DOMDataBroker globalDomDataBroker) {
		this.globalDomDataBroker = globalDomDataBroker;
	}

	public String generateSubscriptionId() {
		this.sub_id++;
		return "20" + Integer.toString(this.sub_id);
	}

	public void createSubscriptionDataStore() {
		DOMDataWriteTransaction tx = this.globalDomDataBroker.newWriteOnlyTransaction();
		NodeIdentifier subscriptions = NodeIdentifier.create(Subscriptions.QNAME);
		NodeIdentifier subscription = NodeIdentifier.create(Subscription.QNAME);

		YangInstanceIdentifier iid = YangInstanceIdentifier.builder().node(Subscriptions.QNAME).build();
		// Creates container node push-update in BI way and
		// commit to MD-SAL at the start of the application.
		ContainerNode cn = Builders.containerBuilder().withNodeIdentifier(subscriptions).build();
		tx.merge(LogicalDatastoreType.OPERATIONAL, iid, cn);
		LOG.info("Transaction going to submit");
		try {
			tx.submit().checkedGet();
		} catch (TransactionCommitFailedException e1) {
			e1.printStackTrace();
		}
		// Creates push-update list node and BI way and
		// commit to MD-SAL at the start of the application.
		YangInstanceIdentifier iid_1 = iid.node(Subscription.QNAME);
		MapNode mn = Builders.mapBuilder().withNodeIdentifier(subscription).build();
		DOMDataWriteTransaction tx_1 = this.globalDomDataBroker.newWriteOnlyTransaction();
		tx_1.merge(LogicalDatastoreType.OPERATIONAL, iid_1, mn);
		try {
			tx_1.submit().checkedGet();
		} catch (TransactionCommitFailedException e1) {
			e1.printStackTrace();
		}
	}

	// Infos need to be stored to MD-SAL and locally.
	public void updateMdSal(SubscriptionInfo subscriptionInfo, operations type) {
		// Storing files to MD-SAL
		// TODO Check if storing the data is correct.
		// NodeIdentifier subscriptionId =
		// NodeIdentifier.create(QName.create(Subscriptions.QNAME,
		// "subscription-id"));
		NodeIdentifier encoding = NodeIdentifier.create(QName.create(Encodings.QNAME, "encoding"));
		// NodeIdentifier updateTrigger =
		// NodeIdentifier.create(QName.create(Subscriptions.QNAME,
		// "update-trigger"));
		// TODO IT WORKS!!!!
		NodeIdentifier stream = NodeIdentifier.create(QName.create(NOTIF_BIS, NOTIF_BIS_DATE, "stream"));
		NodeIdentifier startTime = NodeIdentifier.create(QName.create(NOTIF_BIS, NOTIF_BIS_DATE, "startTime"));
		NodeIdentifier stopTime = NodeIdentifier.create(QName.create(NOTIF_BIS, NOTIF_BIS_DATE, "stopTime"));
		NodeIdentifier subStartTime = NodeIdentifier.create(QName.create("urn:ietf:params:xml:ns:yang:ietf-yang-push", NOTIF_BIS_DATE, "subscription-start-time"));
		
		YangInstanceIdentifier pid = YangInstanceIdentifier.builder().node(Subscriptions.QNAME).node(Subscription.QNAME)
				.build();

		NodeIdentifierWithPredicates p = new NodeIdentifierWithPredicates(
				QName.create(Subscriptions.QNAME, "subscription"), QName.create(Subscriptions.QNAME, "subscription-id"),
				sub_id);

		MapEntryNode men = ImmutableNodes.mapEntryBuilder().withNodeIdentifier(p)
				// .withChild(ImmutableNodes.leafNode(subscriptionId, sub_id))
				// .withChild(ImmutableNodes.leafNode(updateTrigger,
				// subscriptionInfo.getUpdateTrigger()))
				// .withChild(ImmutableNodes.leafNode(stream,
				// prefix+subscriptionInfo.getStream())
				.withChild(ImmutableNodes.leafNode(stream, subscriptionInfo.getStream()))
				.withChild(ImmutableNodes.leafNode(subStartTime, subscriptionInfo.getSubscriptionStartTime()))
				.withChild(ImmutableNodes.leafNode(startTime, subscriptionInfo.getStartTime()))
				
//				.withChild(ImmutableNodes.leafNode(stopTime, subscriptionInfo.getStopTime()))
				.withChild(ImmutableNodes.leafNode(encoding, subscriptionInfo.getEncoding())).build();

		DOMDataWriteTransaction tx = this.globalDomDataBroker.newWriteOnlyTransaction();
		YangInstanceIdentifier yid = pid
				.node(new NodeIdentifierWithPredicates(Subscription.QNAME, men.getIdentifier().getKeyValues()));

		// Distinguish whether if a subscription has to be established,
		// deleted or modified in MD-SAL data store.
		switch (type) {
		case establish:
			if (!checkIfSubscriptionExists(subscriptionInfo.getSubscriptionId())) {
				tx.merge(LogicalDatastoreType.OPERATIONAL, yid, men);
				// Storing files locally
				masterSubMap.put(subscriptionInfo.getSubscriptionId(), subscriptionInfo);
				LOG.info("Subscription stored...");
			} else {
				LOG.info("Subscription already exists");
			}
			break;
		case delete:
			if (checkIfSubscriptionExists(subscriptionInfo.getSubscriptionId())) {
				// tx.delete(LogicalDatastoreType.CONFIGURATION, yid);
				masterSubMap.remove(subscriptionInfo.getSubscriptionId(), subscriptionInfo);
				LOG.info("Subscription has been deleted");
			} else {
				LOG.info("Subscription didn't exist");
			}
			break;
		case modify:
			if (checkIfSubscriptionExists(subscriptionInfo.getSubscriptionId())) {
				// tx.merge(LogicalDatastoreType.CONFIGURATION, yid, men);
				masterSubMap.put(subscriptionInfo.getSubscriptionId(), subscriptionInfo);
				LOG.info("Subscription modified...");
			} else {
				LOG.info("Subscription didn't exist");
			}
			break;
		default:
			break;
		}
		try {
			tx.submit().checkedGet();
			LOG.info("Transaction has been submitted");
		} catch (TransactionCommitFailedException e) {
			e.printStackTrace();
			LOG.info("Uuups");
		}
		LOG.info("MD-SAL has been updated");
	}

	public Boolean checkIfSubscriptionExists(String sub_id) {
		if (masterSubMap.get(sub_id) == null) {
			LOG.info("Subscription not existing");
			return false;
		}
		LOG.info("Subscription existing");
		return true;
	}

	public SubscriptionInfo getSubscription(String subscriptionID) {
		return this.masterSubMap.get(subscriptionID);
	}

}
