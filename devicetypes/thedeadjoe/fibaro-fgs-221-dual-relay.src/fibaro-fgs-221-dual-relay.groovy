/**
 *	Author: Robert Tokarski (TheDeadJoe)
 *	email: the.dead.joe@gmail.com
 *	Date: 2018-05-14
 */
 
metadata {

    definition (name: "Fibaro FGS-221 Dual Relay", namespace: "TheDeadJoe", author: "Robert Tokarski") {
        capability "relaySwitch"
        capability "Configuration"
        capability "Refresh"
        capability "Zw Multichannel"

        attribute "switch1", "string"
        attribute "switch2", "string"

        command "on1"
        command "off1"
        command "on2"
        command "off2"

        fingerprint deviceId: "0x1001", inClusters:"0x5E, 0x86, 0x72, 0x5A, 0x85, 0x59, 0x73, 0x25, 0x20, 0x27, 0x71, 0x2B, 0x2C, 0x75, 0x7A, 0x60, 0x32, 0x70"
    }

    simulator {

        status "on": "command: 2003, payload: FF"
        status "off": "command: 2003, payload: 00"

        reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
        reply "200100,delay 100,2502": "command: 2503, payload: 00"
    }

    tiles {

        standardTile("switch1", "device.switch1",canChangeIcon: true) {
            state "on", label: "switch1", action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "off", label: "switch1", action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        standardTile("switch2", "device.switch2",canChangeIcon: true) {
            state "on", label: "switch2", action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "off", label: "switch2", action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("configure", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:"", action:"configure", icon:"st.secondary.configure"
        }

        main(["switch1", "switch2"])

        details(["switch1", "switch2", "refresh", "configure"])
    }
    
	preferences {
    	input "paragraph", "paragraph", description: "Input a parameter to change. Watch the debug logs to verify change", displayDuringSetup: false
    	input "parameter",  "number", title: "Parameter Number", defaultValue: "", displayDuringSetup: false, required: false
        input "parameterValue",  "number", title: "Parameter Value", defaultValue: "", displayDuringSetup: false, required: false
        input "associateGroup",  "number", title: "Associate SmartThings with this group", defaultValue: "", displayDuringSetup: false, required: false
  	}
}

def parse(String description) {

    def result = []
    
    def cmd = zwave.parse(description)
    
    if (cmd) {
        log.debug "Parsed ${cmd} to ${result.inspect()}"
        result += zwaveEvent(cmd)
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {

    def result = []

	result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()

    response(delayBetween(result, 1000))
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) {

    if (cmd.endPoint == 2 ) {
    
        def currstate = device.currentState("switch2").getValue()
        
        if (currstate == "on") {
        	sendEvent(name: "switch2", value: "off", isStateChange: true, display: false)
        } else if (currstate == "off") {
        	sendEvent(name: "switch2", value: "on", isStateChange: true, display: false)
        }
        
        response([zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()])
        
    } else if (cmd.endPoint == 1 ) {
    
        def currstate = device.currentState("switch1").getValue()
        
        if (currstate == "on") {
        	sendEvent(name: "switch1", value: "off", isStateChange: true, display: false)
        } else if (currstate == "off") {
        	sendEvent(name: "switch1", value: "on", isStateChange: true, display: false)
        } 
        
        response([zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()])
    }   
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {

	def map = [ name: "switch$cmd.sourceEndPoint" ]
    
	if (cmd.commandClass == 37) {
    
        if (cmd.parameter == [0]) {
            map.value = "off"
        } else if (cmd.parameter == [255]) {
            map.value = "on"
        }
        
		def childDevice = getChildDeviceForEndpoint(cmd.sourceEndPoint);
        
		if (childDevice) {
        	def formatCmd = ([cmd.commandClass, cmd.command] + cmd.parameter).collect{ String.format("%02X", it) }.join()
			childDevice.handleEvent(formatCmd)
		} 
        
        createEvent(map)
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "Non handled cmd: ${device.displayName}: ${cmd}"
    createEvent(descriptionText: "${device.displayName}: ${cmd}")
}

def refresh() {

	def cmds = []

	cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
	
    delayBetween(cmds, 1000)
}

def configure() {

    def cmds = []
    
    if (parameter != null && parameterValue != null) {
        if (parameter.value != "" && parameterValue.value != ""){
            cmds << zwave.configurationV1.configurationGet(parameterNumber: parameter.value).format()
            cmds << zwave.configurationV1.configurationSet(parameterNumber: parameter.value, configurationValue: [parameterValue.value]).format()	// Set switch to report values for both Relay1 and Relay2
            cmds << zwave.configurationV1.configurationGet(parameterNumber: parameter.value).format()
        } 
    }
    
    if (associateGroup != null) {
        if (associateGroup.value != "") {
            cmds << zwave.associationV2.associationGet(groupingIdentifier: associateGroup.value).format()
            cmds << zwave.associationV2.associationSet(groupingIdentifier: associateGroup.value, nodeId:[zwaveHubNodeId]).format()
            cmds << zwave.associationV2.associationGet(groupingIdentifier: associateGroup.value).format()
        } 
    }
    
    if (cmds != [] && cmds != null) return delayBetween(cmds, 2000) else return
}

def updated() {
    def cmds = configure()
    response(cmds)
}

def on1() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[255]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ], 1000)
}

def off1() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[0]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ], 1000)
}

def on2() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[255]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def off2() {
    delayBetween([
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[0]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format()
    ], 1000)
}

def sendCommand(endpointDevice, commands) {

	def result
    
	if (commands instanceof String) {
		commands = commands.split(',') as List
	}
    
	def endpoint = deviceEndpointNumber(endpointDevice)
	
    if (endpoint) {
        
		result = commands.collect { cmd ->
			if (cmd.startsWith("delay")) {
				new physicalgraph.device.HubAction(cmd)
			} else {
				new physicalgraph.device.HubAction(encap(cmd, endpoint))
			}
		}
        
        log.debug "sendCommand ${result}"
        
		sendHubCommand(result, 0)
	}
}

private getChildDeviceForEndpoint(Integer endpoint) {

	def children = childDevices

	if (children && endpoint) {
		return children.find{ it.deviceNetworkId.endsWith("ep$endpoint") }
	}
}

private deviceEndpointNumber(device) {

	String dni = device.deviceNetworkId
	
    if (dni.size() >= 5 && dni[2..3] == "ep") {
		return device.deviceNetworkId[4..-1].toInteger()
	} else if (dni.size() >= 6 && dni[2..4] == "-ep") {
		return device.deviceNetworkId[5..-1].toInteger()
	}
}

private encap(cmd, endpoint) {

	if (endpoint) {
    
		if (cmd instanceof physicalgraph.zwave.Command) {
        
			zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd).format()
            
		} else {
        
			def header = state.sec ? "988100600D00" : "600D00"
			String.format("%s%02X%s", header, endpoint, cmd)
		}
        
	} else {
		cmd.format()
	}
}