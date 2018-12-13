/**
 *	Author: Robert Tokarski (TheDeadJoe)
 *	email: the.dead.joe@gmail.com
 *	Date: 2018-05-14
 */
 
preferences {
    section("Network") {
        input "ip", "text", title: "IP", description: "(ie. 192.168.0.15)", required: true
        input "port", "text", title: "Port", description: "(ie. 80)", required: true
        input "relay", "text", title: "Relay", description: "(ie. switch)", required: true
    }
}

metadata {
    definition(name: "Sonoff Basic Relay with AFE firmware", namespace: "TheDeadJoe", author: "Robert Tokarski") {
        capability "switch"
        
        command "refresh"
    }

    standardTile("Switch", "device.switch", canChangeIcon: true) {
        state "on", label: "on", action: "off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
        state "off", label: "off", action: "on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
    }
    
    standardTile("Refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
        state "default", label: "", action: "refresh", icon:"st.secondary.refresh"
    }    

    main(["Switch"])
    
    details(["Switch", "Refresh"])
}

def request(command) {
    def path = "/?device=relay&name=${relay}&command=" + command

    def action = new physicalgraph.device.HubAction(
    	[
        headers: [HOST: "${ip}:${port}"],
        method: "GET",
        path: path,
        ],
        null,
        [callback: response]
    )

    sendHubCommand(action)
}

def response(physicalgraph.device.HubResponse hubResponse) {
    def body = hubResponse.json
    
    sendEvent(name: "switch", value: body.value);
}

def installed() {
	updated()
}

def updated() {
    unschedule()
    
    if (settings.ip != null && settings.port != null && settings.relay != null) {
        device.deviceNetworkId = "${settings.ip}:${settings.port}"
		runEvery1Minute(refresh)
    }
}

def parse(String description) {
    return null
}

def off() {
    request("off")
}

def on() {
    request("on")
}

def refresh() {
	request("get")
}