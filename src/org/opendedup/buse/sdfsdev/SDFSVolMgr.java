/*******************************************************************************
 * Copyright (C) 2016 Sam Silverberg sam.silverberg@gmail.com	
 *
 * This file is part of OpenDedupe SDFS.
 *
 * OpenDedupe SDFS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenDedupe SDFS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.opendedup.buse.sdfsdev;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.opendedup.buse.driver.BUSEMkDev;
import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.servers.SDFSService;
import org.opendedup.util.OSValidator;

public class SDFSVolMgr implements Daemon {
	private static SDFSService sdfsService = null;
	private static int port;
	private static boolean useSSL;
	private static String password = null;

	public static Options buildOptions() {
		Options options = new Options();
		options.addOption("d", false, "verbose debug output");
		options.addOption("v", true, "sdfs volume to mount \ne.g. dedup");
		options.addOption("p", true, "port to use for sdfs cli");
		options.addOption("e", true, "password to decrypt config");
		options.addOption("vc", true,
				"sdfs volume configuration file to mount \ne.g. /etc/sdfs/dedup-volume-cfg.xml");
		options.addOption("c", false,
				"sdfs volume will be compacted and then exit");
		options.addOption(
				"forcecompact",
				false,
				"sdfs volume will be compacted even if it is missing blocks. This option is used in conjunction with -c");
		options.addOption(
				"rv",
				true,
				"comma separated list of remote volumes that should also be accounted for when doing garbage collection. "
						+ "If not entered the volume will attempt to identify other volumes in the cluster.");
		options.addOption("h", false, "displays available options");
		options.addOption("nossl", false,
				"If set ssl will not be used sdfscli traffic.");
		options.addOption("nocheck", false,
				"Will disable block consistancy check on startup.");
		return options;
	}

	public static void main(String[] args) throws ParseException {
		setup(args);
		try {
			sdfsService.start(useSSL, port,password);
		} catch (Throwable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Exiting because " + e1.toString());
			System.exit(-1);
		}
		try {
			SDFSLogger.getLog().info("Volume name is " + Main.volume.getName());
			VolumeShutdownHook.service = sdfsService;
			VolumeShutdownHook shutdownHook = new VolumeShutdownHook();
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	private static void setup(String[] args) throws ParseException {
		checkJavaVersion();
		port = -1;
		String volumeConfigFile = null;
		CommandLineParser parser = new PosixParser();
		Options options = buildOptions();
		useSSL = true;
		CommandLine cmd = parser.parse(options, args);
		ArrayList<String> volumes = new ArrayList<String>();
		if (cmd.hasOption("h")) {
			printHelp(options);
			System.exit(1);
		}
		if (cmd.hasOption("rv")) {
			StringTokenizer st = new StringTokenizer(cmd.getOptionValue("rv"),
					",");
			while (st.hasMoreTokens()) {
				volumes.add(st.nextToken());
			}
		}
		if (cmd.hasOption("p")) {
			port = Integer.parseInt(cmd.getOptionValue("p"));
		}
		String volname = "SDFS";
		if (cmd.hasOption("nocheck")) {
			Main.runConsistancyCheck = false;
		}
		if (cmd.hasOption("e")) {
			password = cmd.getOptionValue("e");
		}
		if (cmd.hasOption("c")) {
			Main.runCompact = true;
			if (cmd.hasOption("forcecompact"))
				Main.forceCompact = true;
		}
		if (cmd.hasOption("v")) {
			File f = new File("/etc/sdfs/" + cmd.getOptionValue("v").trim()
					+ "-volume-cfg.xml");
			volname = f.getName();
			if (!f.exists()) {
				System.out.println("Volume configuration file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			volumeConfigFile = f.getPath();
		} else if (cmd.hasOption("vc")) {
			File f = new File(cmd.getOptionValue("vc").trim());
			volname = f.getName();
			if (!f.exists()) {
				System.out.println("Volume configuration file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			volumeConfigFile = f.getPath();
		} else {
			File f = new File("/etc/sdfs/" + args[0].trim() + "-volume-cfg.xml");
			volname = f.getName();
			if (!f.exists()) {
				System.out.println("Volume configuration file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			volumeConfigFile = f.getPath();
		}
		if (cmd.hasOption("nossl")) {
			useSSL = false;
		}

		if (volumeConfigFile == null) {
			System.out
					.println("error : volume or path to volume configuration file not defined");
			printHelp(options);
			System.exit(-1);
		}
		if (OSValidator.isUnix())
			Main.logPath = "/var/log/sdfs/" + volname + ".log";
		if (OSValidator.isWindows())
			Main.logPath = Main.volume.getPath() + "\\log\\"
					+ Main.volume.getName() + ".log";
		Main.blockDev = true;

		BUSEMkDev.init();
		sdfsService = new SDFSService(volumeConfigFile, volumes);
		VolumeShutdownHook.service = sdfsService;
		VolumeShutdownHook shutdownHook = new VolumeShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		if (cmd.hasOption("d")) {
			SDFSLogger.setLevel(0);
		}

	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"startvolmgr "
								+ "-[v|vc] <volume name to mount | path to volume config file> -p <TCP Management Port> -rv <comma separated list of remote volumes> ",
						options);
	}

	private static void checkJavaVersion() {
		Properties sProp = java.lang.System.getProperties();
		String sVersion = sProp.getProperty("java.version");
		sVersion = sVersion.substring(0, 3);
		Float f = Float.valueOf(sVersion);
		if (f.floatValue() < (float) 1.7) {
			System.out.println("Java version must be 1.7 or newer");
			System.out
					.println("To get Java 7 go to https://jdk7.dev.java.net/");
			System.exit(-1);
		}
	}

	@Override
	public void destroy() {
		sdfsService = null;

	}

	@Override
	public void init(DaemonContext arg0) throws DaemonInitException, Exception {
		setup(arg0.getArguments());

	}

	@Override
	public void start() throws Exception {
		sdfsService.start(useSSL, port,password);

	}

	@Override
	public void stop() throws Exception {
		SDFSLogger.getLog().info("Please Wait while shutting down SDFS");
		SDFSLogger.getLog().info("Data Can be lost if this is interrupted");
		sdfsService.stop();
		try {
			try {
				Main.volume.closeAllDevices();
			} catch (Exception e) {
			}
			Thread.sleep(1000);
			try {
				BUSEMkDev.release();
			} catch (Exception e) {
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		SDFSLogger.getLog().info("SDFS Shut Down Cleanly");
		SDFSLogger.getLog().info("All Data Flushed");

	}
}
