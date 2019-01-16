/**
 *	Author: Robert Tokarski (TheDeadJoe)
 *	email: the.dead.joe@gmail.com
 *	Date: 2018-12-20
 */
 
preferences {
    section("Network") {
        input "ip", "text", title: "IP", description: "(ie. 192.168.0.16)", required: true
    }
}

metadata {
    definition(name: "Shelly 2", namespace: "TheDeadJoe", author: "Robert Tokarski") {
    	capability "Switch"
        capability "Power Meter"
        capability "Refresh"      
    }
   
	tiles {
    	standardTile("switch", "device.switch", canChangeIcon: true) {
        	state "off", label: "off", action: "on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        	state "on", label: "on", action: "off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
    	}
            
        valueTile("power", "device.power") {
            state "power", label:'${currentValue}\n W'
        }    

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label: "", action: "refresh", icon:"st.secondary.refresh"
        }   

        main(["switch"])

        details(["switch", "power", "refresh"])
    }
}

def request(path) {
    def action = new physicalgraph.device.HubAction(
    	[
        headers: [HOST: "${ip}:80"],
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

	if (body.containsKey("relays")) {
        body.relays.eachWithIndex { relay, idx ->
        	if (idx == 0) {
            	sendEvent(name: "switch", value: relay.ison == true ? "on" : "off");
            } else {
            	def childDevices = getChildDevices()
				def childDevice = childDevices.find{it.deviceNetworkId.endsWith("-${idx + 1}")}
				childDevice.sendEvent(name: "switch", value: relay.ison == true ? "on" : "off");
			}
        }
	}
    
    if (body.containsKey("meters")) {
    	sendEvent(name: "power", value: body.meters[0].power);	
	}    
}

def installed() {
    updated()
}

def updated() {
	unschedule()
    
    if (settings.ip != null) {
        device.deviceNetworkId = "${settings.ip}"
		runEvery1Minute(refresh)
    }
    
    def childDevices = getChildDevices()

	if (childDevices.size() == 0) {
        addChildDevice("Shelly Relay", "${device.deviceNetworkId}-2", null, [label: "${device.displayName} (CH2)", isComponent: false])
    }
}

def parse(String description) {
    return null
}

def on() {
    request("/relay/0?turn=on")
    refresh()
}

def off() {
    request("/relay/0?turn=off")
    refresh()
}

def childOn(String dni) {
    request("/relay/1?turn=on")
    refresh()
}

def childOff(String dni) {
    request("/relay/1?turn=off")
    refresh()
}

def refresh() {
    request("/status")
}