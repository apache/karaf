/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.main.lock;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class is used to create the sql statements for the karaf lock tables that are used
 * for clustering of karaf instances.
 * 
 * It will generate sql statement to create two separate tables, a lock table and a lock id table
 * 
 *   CREATE TABLE LOCK ( ID INTEGER DEFAULT 0, STATE INTEGER DEFAULT 0, LOCK_DELAY INTEGER DEFAULT 0 )
 *   
 *   CREATE TABLE LOCK_ID ( ID INTEGER DEFAULT 0 )
 *   
 * @author Claudio Corsi
 * 
 */
public class GenericStatements {

	private final String lockTableName;
	private final String lockIdTableName;
	private final String clusterName;

	/**
	 * This constructor is used to determine the name of the karaf lock table, the karaf lock id
	 * table and the name of the clustered instances.
	 *
	 * @param lockTableName The name of the karaf lock table
	 * @param lockIdTableName The name of the karaf lock id table
	 * @param clusterName the name of the cluster being used
	 */
	public GenericStatements(String lockTableName, String lockIdTableName, String clusterName) {
		this.lockTableName   = lockTableName;
		this.lockIdTableName = lockIdTableName;
		this.clusterName     = clusterName;
	}

	/**
	 * This method will return the name of the cluster that the instances are using to compete for the
	 * master lock.
	 *
	 * @return cluster node name
	 */
	public final String getNodeName() {
		return this.clusterName;
	}

	/**
	 * This method will return the name of the karaf lock table.
	 *
	 * @return name of the karaf lock table
	 */
	public final String getLockTableName() {
		return lockTableName;
	}

	/**
	 * This method will return the insert statement used to create a row in the Lock table and will
	 * generate the following sql statement.
	 *
	 * INSERT INTO KARAF_LOCK (ID, STATE, LOCK_DELAY) VALUES (0, 0, 0)
	 * 
	 * @return sql insert statement
	 */
	private String getLockTableInitialInsertStatement() {
		return "INSERT INTO " + this.getLockTableName() + "(ID, STATE, LOCK_DELAY) VALUES (0, 0, 0)";
	}

	/**
	 * This will be called when trying to acquire the lock and will generate the following sql statemnt.
	 *
	 *  UPDATE KARAF_LOCK SET ID = ?, STATE = ?, LOCK_DELAY = ? WHERE ID = 0 OR ID = ?
	 * 
	 * You are then expected to assign the values associated with the sql statement.
	 *
	 * @return sql update statement
	 */
	public String getLockUpdateIdStatement(int id, int state, int lock_delay, int curId) {
		return String.format("UPDATE %s SET ID = %d, STATE = %d, LOCK_DELAY = %d WHERE ID = 0 OR ID = %d", 
							 this.getLockTableName(), id, state, lock_delay, curId);
	}

	/**
	 * This will be called when trying to steal the lock and will generate the following sql statemnt.
	 *
	 *  UPDATE KARAF_LOCK SET ID = ?, STATE = ?, LOCK_DELAY = ? WHERE ( ID = 0 OR ID = ? ) AND STATE = ?
	 *
	 * You are then responsible to assign the values of the different fields using standard jdbc statement
	 * calls.
	 * 
	 * @return sql update statement
	 */
	public String getLockUpdateIdStatementToStealLock(int id, int state, int lock_delay, int curId, int curState) {
		return String.format("UPDATE %s SET ID = %d, STATE = %d, LOCK_DELAY = %d WHERE ( ID = 0 OR ID = %d ) AND STATE = %d", 
							 this.getLockTableName(), id, state, lock_delay, curId, curState) ;
	}

	/**
	 * This method is called only when we are releasing the lock and will generate the following sql
	 * statement.
	 *
	 *  UPDATE KARAF_LOCK SET ID = 0 WHERE ID = ?
	 * 
	 * @return sql update statement
	 */
	public String getLockResetIdStatement(int id) {
		return String.format("UPDATE %s SET ID = 0 WHERE ID = %d", this.getLockTableName(), id);
	}

	/**
	 * This will be called to determine the current master instance for the lock table and will 
	 * generate the following sql statement.
	 *
	 * SELECT ID, STATE, LOCK_DELAY FROM KARAF_LOCK
	 *
	 * @return sql select statement
	 */
	public String getLockSelectStatement() {
		return "SELECT ID, STATE, LOCK_DELAY FROM " + this.getLockTableName();
	}

	public int getIdFromLockSelectStatement(ResultSet rs) throws SQLException {
		return rs.getInt(1);
	}

	public int getStateFromLockSelectStatement(ResultSet rs) throws SQLException {
		return rs.getInt(2);
	}

	public int getLockDelayFromLockSelectStatement(ResultSet rs) throws SQLException {
		return rs.getInt(3);
	}

	/**
	 * This method should only be called during the creation of the KARAF_LOCK table and will
	 * generate the following sql statement.
	 *
	 * CREATE TABLE KARAF_LOCK (ID INTEGER DEFAULT 0, STATE INTEGER DEFAULT 0, LOCK_DELAY INTEGER DEFAULT 0)
	 * 
	 * @return sql create table statement
	 */
	private String getLockTableCreateStatement() {
		return "CREATE TABLE " + this.getLockTableName() 
			   + " ( ID INTEGER DEFAULT 0, STATE INTEGER DEFAULT 0 , LOCK_DELAY INTEGER DEFAULT 0 )";
	}

	
	//  ==================  LOCK ID TABLE ========================

	/**
	 * This method will generate the create table sql statement to create the karaf id table and will
	 * generate the following sql statement.
	 *
	 * CREATE TABLE KARAF_ID ( ID INTEGER DEFAULT 0 )
	 *
	 * @return sql create table statement
	 */
	private String getLockIdTableCreateStatement() {
		return "CREATE TABLE " + this.getLockIdTableName() 
			    + " ( ID INTEGER DEFAULT 0 )";
	}
	
	/**
	 * This method will return the sql statement to retreive the id of the lock id table and will
	 * generate the following sql statement.
	 *
	 * SELECT ID FROM KARAF_ID
	 *
	 * @return sql select statement
	 */
	public String getLockIdSelectStatement() {
		return "SELECT ID FROM " + this.getLockIdTableName();
	}

	public int getIdFromLockIdSelectStatement(ResultSet rs) throws SQLException {
		return rs.getInt(1);
	}

	/**
	 * This method will return the update statement for the lock id table and will generate the
	 * following sql statement.
	 *
	 * UPDATE KARAF_ID SET ID = ? WHERE ID = ?
	 *
	 * @return sql update statement
	 */
	public String getLockIdUpdateIdStatement(int id, int curId) {
		return String.format("UPDATE %s SET ID = %d WHERE ID = %d", this.getLockIdTableName(), id, curId);
	}
	
	/**
	 * This method will return the name of the karaf lock id table.
	 *
	 * @return name of the karaf lock id table
	 */
	public final String getLockIdTableName() {
		return lockIdTableName;
	}

	/**
	 * This method will return the required sql statements to initialize the lock database.
	 *
	 * @return array of sql statements
	 */
	public String[] getLockCreateSchemaStatements(long moment) {
		return new String[] {
			getLockTableCreateStatement(),
			getLockIdTableCreateStatement(),
			getLockTableInitialInsertStatement(),
			getLockIdTableInitialInsertStatement(),
		};
	}

	/**
	 * This method will return the insert statement to insert a row in the lock id table and will
	 * generate the following sql statement.
	 *
	 * INSERT INTO KARAF_ID (ID) VALUES (0)
	 *
	 * @return sql insert statement
	 */
	private String getLockIdTableInitialInsertStatement() {
		return "INSERT INTO " + this.getLockIdTableName() + "(ID) VALUES (0)";
	}

}
