/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
 */
package strat.mining.stratum.proxy.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.utils.HashrateUtils;
import strat.mining.stratum.proxy.worker.WorkerConnection;

/**
 * Represent a user of this proxy
 * 
 * @author Strat
 * 
 */
public class User {

	private String name;

	private volatile List<WeakReference<WorkerConnection>> seenOnConnections;

	private Deque<Share> lastAcceptedShares;
	private Deque<Share> lastRejectedShares;

	private Date creationTime;
	private Date lastShareSubmitted;

	private Integer samplingHashesPeriod = Constants.DEFAULT_USER_HASHRATE_SAMPLING_PERIOD * 1000;

	public User(String name) {
		this.name = name;
		creationTime = new Date();
		lastAcceptedShares = new ConcurrentLinkedDeque<Share>();
		lastRejectedShares = new ConcurrentLinkedDeque<Share>();
		seenOnConnections = Collections.synchronizedList(new ArrayList<WeakReference<WorkerConnection>>());
	}

	/**
	 * Add a connection where this user has been authorized.
	 * 
	 * @param workerConnection
	 */
	public void addConnection(WorkerConnection workerConnection) {
		seenOnConnections.add(new WeakReference<WorkerConnection>(workerConnection));
		purgeAndGetConnectionList();
	}

	/**
	 * Return a list of connections were this user has been authorized.
	 * 
	 * @return
	 */
	public List<WorkerConnection> getWorkerConnections() {
		return purgeAndGetConnectionList();
	}

	/**
	 * Return the of accepted hashes per seconds of the user.
	 * 
	 * @return
	 */
	public double getAcceptedHashrate() {
		HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastAcceptedShares, samplingHashesPeriod);
	}

	/**
	 * Return the number of rejected hashes per seconds of the user.
	 * 
	 * @return
	 */
	public double getRejectedHashrate() {
		HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		return HashrateUtils.getHashrateFromShareList(lastRejectedShares, samplingHashesPeriod);
	}

	/**
	 * Update the shares lists with the given share to compute hashrate
	 * 
	 * @param share
	 * @param isAccepted
	 */
	public void updateShareLists(Share share, boolean isAccepted) {
		if (isAccepted) {
			lastAcceptedShares.addLast(share);
			HashrateUtils.purgeShareList(lastAcceptedShares, samplingHashesPeriod);
		} else {
			lastRejectedShares.addLast(share);
			HashrateUtils.purgeShareList(lastRejectedShares, samplingHashesPeriod);
		}
		lastShareSubmitted = new Date();
	}

	/**
	 * Get the date of creation of the user. The user is created at the first
	 * authorize request.
	 * 
	 * @return
	 */
	public Date getCreationTime() {
		return creationTime;
	}

	public void setSamplingHashesPeriod(Integer samplingHashesPeriod) {
		this.samplingHashesPeriod = samplingHashesPeriod * 1000;
	}

	public String getName() {
		return name;
	}

	public Date getLastShareSubmitted() {
		return lastShareSubmitted;
	}

	/**
	 * Return a list of active connections and remove no more existing ones.
	 * 
	 * @return
	 */
	private List<WorkerConnection> purgeAndGetConnectionList() {
		List<WorkerConnection> newConnectionList = new ArrayList<WorkerConnection>();
		synchronized (seenOnConnections) {
			Iterator<WeakReference<WorkerConnection>> iterator = seenOnConnections.iterator();
			WeakReference<WorkerConnection> weakReference = null;
			while (iterator.hasNext()) {
				weakReference = iterator.next();
				if (weakReference.get() != null && weakReference.get().isConnected()) {
					newConnectionList.add(weakReference.get());
				} else {
					iterator.remove();
				}
			}
		}

		return newConnectionList;
	}

}
