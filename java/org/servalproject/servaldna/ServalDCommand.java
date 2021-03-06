/**
 * Copyright (C) 2011 The Serval Project
 *
 * This file is part of Serval Software (http://www.servalproject.org)
 *
 * Serval Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.servalproject.servaldna;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServalDCommand
{
	private ServalDCommand(){
	}

	static
	{
		System.loadLibrary("serval");
	}

	public static final int STATUS_ERROR = 255;

	public static String toString(String[] values) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(values[i]);
		}
		return sb.toString();
	}

	// copies the semantics of serval-dna's confParseBoolean
	private static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null || "".equals(value))
			return defaultValue;
		return "off".compareToIgnoreCase(value) != 0
				&& "no".compareToIgnoreCase(value) != 0
				&& "false".compareToIgnoreCase(value) != 0
				&& "0".compareToIgnoreCase(value) != 0;
	}

	/**
	 * Low-level JNI entry point into servald command line.
	 *
	 * @param results	Interface that will receive each value from the command
	 * @param args		The words to pass on the command line (ie, argv[1]...argv[n])
	 * @return			The servald exit status code (normally 0 indicates success)
	 */
	private static native int rawCommand(IJniResults results, String[] args);

	/**
	 * Common entry point into servald command line.
	 *
	 * @param callback
	 *            Each result will be passed to callback.result(String)
	 *            immediately.
	 * @param args
	 *            The parameters as passed on the command line, eg: res =
	 *            servald.command("config", "set", "debug", "peers");
	 * @return The servald exit status code (normally0 indicates success)
	 */
	public static synchronized int command(final IJniResults callback, String... args)
			throws ServalDFailureException {
		int ret = ServalDCommand.rawCommand(callback, args);
		if (ret == STATUS_ERROR)
			throw new ServalDFailureException("Command \"" + toString(args)+"\" returned an error");
		return ret;
	}

	public static synchronized JniResult command(String... args)
			throws ServalDFailureException {
		JniResult result = new JniResult();
		result.setCommand(args);
		result.setResult(ServalDCommand.rawCommand(result, args));
		return result;
	}

	public static class Status extends JniResult{
		public int pid;
		public int tries;
		public String instancePath;
		public String status;
		public int mdpInetPort;
		public int httpPort;

		@Override
		public void putString(String value) {
			if (columnName.equals("instancepath"))
				instancePath=value;
			if (columnName.equals("status"))
				status=value;
			if (columnName.equals("mdp_inet_port"))
				mdpInetPort=Integer.parseInt(value);
			if (columnName.equals("http_port"))
				httpPort=Integer.parseInt(value);
		}

		@Override
		public void putLong(long value) {
			if (columnName.equals("pid"))
				pid = (int)value;
			if (columnName.equals("tries"))
				tries = (int)value;
		}

		@Override
		public String toString() {
			return "Status{" +
					"pid=" + pid +
					", tries=" + tries +
					", instancePath='" + instancePath + '\'' +
					", status='" + status + '\'' +
					", mdpInetPort='" + mdpInetPort + '\'' +
					", httpPort='" + httpPort + '\'' +
					'}';
		}
	}

	/** Start the servald server process if it is not already running.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static Status serverStart()
			throws ServalDFailureException {
		Status result = new Status();
		result.setResult(command(result, "start"));
		return result;
	}

	public static Status serverStart(String execPath)
			throws ServalDFailureException {
		Status result = new Status();
		result.setResult(command(result, "start", "exec", execPath));
		return result;
	}

	public static Status serverStop()
			throws ServalDFailureException {
		Status result = new Status();
		result.setResult(command(result, "stop"));
		return result;
	}

	public static Status serverStatus()
			throws ServalDFailureException {
		Status result = new Status();
		result.setResult(command(result, "status"));
		return result;
	}

	public static class IdentityResult extends JniResult {
		public String did;
		public String name;
		public SubscriberId subscriberId;

		@Override
		public void putString(String value) {
			if (this.columnName.equals("did"))
				this.did = value;
			if (this.columnName.equals("name"))
				this.name = value;
			if (this.columnName.equals("sid"))
				try {
					this.subscriberId = new SubscriberId(value);
				} catch (AbstractId.InvalidHexException e) {
					e.printStackTrace();
				}
		}

		@Override
		public void putBlob(byte[] value) {
			if (this.columnName.equals("sid"))
				try {
					this.subscriberId = new SubscriberId(value);
				} catch (AbstractId.InvalidBinaryException e) {
					e.printStackTrace();
				}
		}

		@Override
		public String toString() {
			return "IdentityResult{" +
					"did='" + did + '\'' +
					", name='" + name + '\'' +
					", subscriberId=" + subscriberId +
					'}';
		}
	}

	public static IdentityResult keyringAdd()
			throws ServalDFailureException {
		IdentityResult result = new IdentityResult();
		command(result, "keyring", "add");
		return result;
	}

	public static IdentityResult keyringSetDidName(SubscriberId sid, String did, String name) throws ServalDFailureException
	{
		IdentityResult result = new IdentityResult();
		command(result, "keyring","set","did", sid.toHex(), did==null?"":did, name==null?"":name);
		return result;
	}

	public static int keyringList(final AsyncResult<IdentityResult> results) throws ServalDFailureException
	{
		return keyringList(new JniResultList<IdentityResult>(results) {
			@Override
			public IdentityResult create() {
				return new IdentityResult();
			}
		});
	}

	public static int keyringList(IJniResults results) throws ServalDFailureException{
		return command(results, "keyring", "list");
	}

	public static IdentityResult reverseLookup(final SubscriberId sid) throws ServalDFailureException {
		IdentityResult result = new IdentityResult();
		command(result, "reverse", "lookup", sid.toHex());
		return result;
	}

	public static class LookupResult extends JniResult {
		public SubscriberId subscriberId;
		public String did;
		public String name;
		public String uri;

		@Override
		public void putString(String value) {
			if (this.columnName.equals("did"))
				this.did = value;
			if (this.columnName.equals("name"))
				this.name = value;
			if (this.columnName.equals("uri"))
				this.uri = value;
		}

		@Override
		public String toString() {
			return "LookupResult{" +
					"subscriberId=" + subscriberId +
					", did='" + did + '\'' +
					", name='" + name + '\'' +
					", uri='" + uri + '\'' +
					'}';
		}
	}

	public static int dnaLookup(AsyncResult<LookupResult> results, String did, int timeout) throws ServalDFailureException {
		return dnaLookup(new JniResultList<LookupResult>(results) {
			@Override
			public LookupResult create() {
				return new LookupResult();
			}
		}, did, timeout);
	}

	public static int dnaLookup(IJniResults results, String did, int timeout) throws ServalDFailureException {
		return command(results, "dna", "lookup", did, Integer.toString(timeout));
	}
	public static class ManifestResult extends JniResult{
		public BundleId manifestId;
		public long version;
		public long fileSize;
		public FileHash fileHash;
		public BundleKey bundleKey;
		public long date;
		public int crypt;
		public String service;
		public String name;
		public boolean readonly=true;
		public byte[] manifest;
		public String secret;
		public SubscriberId author;
		public long rowId;
		public long insertTime;

		@Override
		public void putString(String value) {
			try {
				if (value!="" && (columnName.equals("manifestid")||columnName.equals("id")))
					manifestId = new BundleId(value);
				if (value!="" && columnName.equals("filehash"))
					fileHash = new FileHash(value);
				if (value!="" && columnName.equals("BK"))
					bundleKey = new BundleKey(value);
				if (value!="" && columnName.equals(".author"))
					author = new SubscriberId(value);
			} catch (AbstractId.InvalidHexException e) {
				e.printStackTrace();
			}
			if (columnName.equals("service"))
				service = value;
			if (columnName.equals("name"))
				name = value;
			if (columnName.equals("secret"))
				secret = value;
		}

		@Override
		public void putBlob(byte[] value) {
			if (columnName.equals("manifest"))
				this.manifest = value;
		}

		@Override
		public void putLong(long value) {
			if (columnName.equals("version"))
				version = value;
			if (columnName.equals("filesize"))
				fileSize = value;
			if (columnName.equals("date"))
				date = value;
			if (columnName.equals("crypt"))
				crypt = (int)value;
			if (columnName.equals(".readonly"))
				readonly = value>0;
			if (columnName.equals(".fromhere"))
				readonly = value==0;
			if (columnName.equals(".rowid") || columnName.equals("_id"))
				rowId = value;
			if (columnName.equals(".inserttime"))
				insertTime = value;
		}
	}

	public static ManifestResult rhizomeAddFile(File payloadPath, File manifestPath, SubscriberId author, String pin)
			throws ServalDFailureException
	{

		List<String> args = new LinkedList<String>();
		args.add("rhizome");
		args.add("add");
		args.add("file");
		if (pin != null) {
			args.add("--entry-pin");
			args.add(pin);
		}
		args.add(author == null ? "" : author.toHex());
		if (payloadPath != null)
			args.add(payloadPath.getAbsolutePath());
		else if (manifestPath != null)
			args.add("");
		if (manifestPath != null)
			args.add(manifestPath.getAbsolutePath());

		ManifestResult result = new ManifestResult();
		result.setResult(command(result, args.toArray(new String[args.size()])));
		return result;
	}

	public static int rhizomeList(AsyncResult<ManifestResult> result, String service, String name, SubscriberId sender, SubscriberId recipient, int offset, int numRows) throws ServalDFailureException {
		return rhizomeList(new JniResultList<ManifestResult>(result) {
			@Override
			public ManifestResult create() {
				return new ManifestResult();
			}
		}, service, name, sender, recipient, offset, numRows);
	}

	public static int rhizomeList(IJniResults result, String service, String name, SubscriberId sender, SubscriberId recipient, int offset, int numRows) throws ServalDFailureException {
		List<String> args = new LinkedList<String>();
		args.add("rhizome");
		args.add("list");
		args.add(service == null ? "" : service);
		args.add(name == null ? "" : name);
		args.add(sender == null ? "" : sender.toHex());
		args.add(recipient == null ? "" : recipient.toHex());
		if (offset > 0)
			args.add("" + offset);
		else if (numRows > 0)
			args.add("0");
		if (numRows > 0)
			args.add("" + numRows);
		return command(result, args.toArray(new String[args.size()]));
	}

	public static ManifestResult rhizomeImportBundle(File payloadFile,
															File manifestFile) throws ServalDFailureException {
		ManifestResult result = new ManifestResult();
		result.setResult(command(result, "rhizome", "import", "bundle",
				payloadFile.getAbsolutePath(), manifestFile.getAbsolutePath()));
		return result;
	}

	public static ManifestResult rhizomeExtractBundle(BundleId manifestId, File manifestFile, File payloadFile) throws ServalDFailureException{
		ManifestResult result = new ManifestResult();
		result.setResult(command(result, "rhizome", "extract", "bundle",
				manifestId.toHex(),
				manifestFile == null ? "-" : manifestFile.getAbsolutePath(),
				payloadFile.getAbsolutePath()));
		return result;
	}

	public static ManifestResult rhizomeExportManifest(BundleId manifestId, File manifestFile) throws ServalDFailureException{
		ManifestResult result = new ManifestResult();
		result.setResult(command(result, "rhizome", "export", "manifest",
				manifestId.toHex(),
				manifestFile == null ? "-" : manifestFile.getAbsolutePath()));
		return result;
	}

	public static ManifestResult rhizomeExtractFile(BundleId manifestId, File payloadFile) throws ServalDFailureException{
		ManifestResult result = new ManifestResult();
		result.setResult(command(result, "rhizome", "extract", "file",
				manifestId.toHex(),
				payloadFile.getAbsolutePath()));
		return result;
	}

	/**
	 * Push Rhizome bundles to all configured direct hosts.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static void rhizomeDirectPush() throws ServalDFailureException
	{
		command("rhizome", "direct", "push");
	}

	/**
	 * Pull Rhizome bundles from all configured direct hosts.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static void rhizomeDirectPull() throws ServalDFailureException
	{
		command("rhizome", "direct", "pull");
	}

	/**
	 * Sync (push and pull) Rhizome bundles from all configured direct hosts.
	 *
	 * @author Andrew Bettison <andrew@servalproject.com>
	 */
	public static void rhizomeDirectSync() throws ServalDFailureException
	{
		command("rhizome", "direct", "sync");
	}

	public static class ConfigItems extends JniResult{
		public Map<String, String> values = new HashMap<String, String>();

		@Override
		public void putString(String value) {
			values.put(this.columnName, value);
		}
	}

	public static ConfigItems getConfig(String pattern) throws ServalDFailureException {
		ConfigItems results = new ConfigItems();
		results.setResult(command(results, "config", "get", pattern));
		return results;
	}

	public static String getConfigItem(String name) throws ServalDFailureException{
		Object result = getConfig(name).values.get(name);
		if (result == null)
			return null;
		if (result instanceof byte[]){
			return new String((byte[])result);
		}
		return result.toString();
	}

	public static void deleteConfig(String name) throws ServalDFailureException {
		ServalDCommand.command("config", "del", name);
	}

	public static void setConfigItem(String name, String value) throws ServalDFailureException {
		ServalDCommand.command("config", "set", name, value);
	}

	public static boolean getConfigItemBoolean(String name, boolean defaultValue) {
		try {
			String value = getConfigItem(name);
			return parseBoolean(value, defaultValue);
		} catch (ServalDFailureException e) {
			e.printStackTrace();
			return defaultValue;
		}
	}

	public static int getConfigItemInt(String name, int defaultValue)  throws ServalDFailureException{
		try {
			return Integer.parseInt(getConfig(name).values.get(name));
		} catch (ServalDFailureException e) {
			e.printStackTrace();
			return defaultValue;
		}
	}

	private static class PeerCount extends JniResult{
		long count;
		@Override
		public void putLong(long value) {
			count = value;
		}
	}

	public static int peerCount() throws ServalDFailureException {
		PeerCount result = new PeerCount();
		result.setResult(ServalDCommand.command(result, "peer", "count"));
		return (int)result.count;
	}

	// Note that the result values will only have a subscriber id
	public static int idPeers(AsyncResult<IdentityResult> results) throws ServalDFailureException {
		return idPeers(new JniResultList<IdentityResult>(results) {
			@Override
			public IdentityResult create() {
				return new IdentityResult();
			}
		});
	}

	public static int idPeers(IJniResults results) throws ServalDFailureException {
		return command(results, "id", "peers");
	}

	public static class Conversation extends JniResult{
		public long id;
		public SubscriberId recipient;
		public String read;
		public long last_message;
		public long read_offset;

		@Override
		public void putString(String value) {
			if (columnName.equals("read"))
				this.read = value;
		}

		@Override
		public void putBlob(byte[] value) {
			if (columnName.equals("recipient"))
				try {
					this.recipient = new SubscriberId(value);
				} catch (AbstractId.InvalidBinaryException e) {
					e.printStackTrace();
				}
		}

		@Override
		public void putLong(long value) {
			if (columnName.equals("_id"))
				this.id = value;
			if (columnName.equals("last_message"))
				this.last_message = value;
			if (columnName.equals("read_offset"))
				this.read_offset = value;
		}
	}

	public static int listConversations(AsyncResult<Conversation> result, final SubscriberId sender, int offset, int numRows) throws ServalDFailureException {
		return listConversations(new JniResultList<Conversation>(result) {
			@Override
			public Conversation create() {
				return new Conversation();
			}
		}, sender, offset, numRows);
	}

	public static int listConversations(IJniResults callback, final SubscriberId sender, int offset, int numRows) throws ServalDFailureException {
		return command(callback, "meshms", "list", "conversations",
				sender.toHex(), ""+offset, ""+numRows);
	}

	public static class Message extends JniResult{
		public long id;
		public long offset;
		public String type;
		public String message;

		@Override
		public void putString(String value) {
			if (columnName.equals("type"))
				this.type = value;
			if (columnName.equals("message"))
				this.message = value;
		}

		@Override
		public void putLong(long value) {
			if (columnName.equals("_id"))
				this.id = value;
			if (columnName.equals("offset"))
				this.offset = value;
		}
	}

	public static int listMessages(AsyncResult<Message> result, final SubscriberId sender, final SubscriberId recipient) throws ServalDFailureException {
		return listMessages(new JniResultList<Message>(result) {
			@Override
			public Message create() {
				return new Message();
			}
		}, sender, recipient);
	}

	public static int listMessages(IJniResults callback, final SubscriberId sender, final SubscriberId recipient) throws ServalDFailureException {
		return ServalDCommand.command(callback, "meshms", "list", "messages",
				sender.toHex(), recipient.toHex());
	}

	public static void sendMessage(final SubscriberId sender, final SubscriberId recipient, String message) throws ServalDFailureException {
		command("meshms", "send", "message",
				sender.toHex(), recipient.toHex(),
				message);
	}

	public static void readMessage(final SubscriberId sender, final SubscriberId recipient) throws ServalDFailureException {
		command("meshms", "read", "messages",
				sender.toHex(), recipient.toHex());
	}

	public static void readMessage(final SubscriberId sender, final SubscriberId recipient, long offset) throws ServalDFailureException {
		command("meshms", "read", "messages",
				sender.toHex(), recipient.toHex(),
				"" + offset);
	}
}
