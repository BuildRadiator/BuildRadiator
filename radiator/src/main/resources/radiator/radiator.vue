<template>
    <div>
        <div id="radiator" style="width: 100%; height: 100%">
            <div v-if="radiatorCodeCorrect" style="width: 100%; height: 100%">
                <table style="width: 100%; height: 100%">
                    <tr style="width: 100%; line-height: 70%">
                        <td style="text-align: center; font-size: 300%" colspan="50">{{title}}<br/>
                            <a style="font-size: 20%" href="//github.com/BuildRadiator/BuildRadiator/wiki/Setting-the-title-and-expanding-step-codes-in-the-UI">change URL to customize the title ↑ or step codes ↓</a>
                            <button style="font-size: 20%" v-on:click="goToEditor">Edit the title and step descriptions</button>

                        </td>
                    </tr>
                    <tr style="width: 100%" v-for="build in rad.builds">
                        <td v-bind:style="'text-align: center; background-color:' + color(build.status) + '; width: ' + rad.minWidth + '%'">
                            <span style="font-size: 200%">{{ build.ref }}</span><br/>
                            <span>{{ build.dur | duration }}</span><br/>
                        </td>
                        <td v-bind:style="'text-align: center; background-color:' + color(step.status) + '; width: ' + rad.columnWidths[ix] + '%'" v-for="(step, ix) in build.steps">
                            <span style="font-size: 200%">{{ step.name }}</span><br/>
                            <span>{{ step.dur | duration }}</span><br/>
                            <span>{{ status(step.status) }}</span>
                        </td>
                    </tr>
                </table>
                <footer>
                    Viewing this radiator is restricted to IP addresses: {{rad.ips}}
                </footer>

            </div>
            <div style="font-size: 200%; height: 100%; display: flex; align-items: center; justify-content: center;" v-if="!radiatorCodeCorrect">
                <div>Radiator code <span style="color: red">{{ code }}</span> not recognized.<br><br>
                    Did you type it correctly?<br><br>
                    Maybe the radiator DOES exist but this <strong>egress</strong><br>TCP/IP address ({{egressIpAddress}}) is not allowed.</div>
            </div>
        </div>
    </div>
</template>
<script>
    var lhash = location.hash;
    var hashParts = lhash.substr(1).split("/");

    if (lhash === "") {
        window.location = "https://github.com/BuildRadiator/BuildRadiator/wiki/Home";
    } else {
        if (hashParts.length < 2) {
            window.location = "https://github.com/BuildRadiator/BuildRadiator/wiki/Setting-the-title-and-expanding-step-codes-in-the-UI";
        }
    }

    function mapReplacements(hashParts) {

        var replacements = {};
        for (var i = 0; i < hashParts.length; i = i +2) {
            if (hashParts[i] !== undefined && hashParts[i+1] !== undefined) {
                replacements[hashParts[i]] = hashParts[i+1].replace(new RegExp("_", 'g'), " ");
            }
        }
        return replacements;
    }

    module.exports = {
        props: ['refreshRate'],
        data: function() {
            return {
                rad: {
                    builds: [],
                    lastUpdated: "",
					ips: [],
                    columnWidths: []
				},
				radiatorCodeCorrect: true,
                egressIpAddress: "",
                code: hashParts.shift(),
				title: hashParts.shift().replace(new RegExp("_", 'g'), " "),
				replacements: mapReplacements(hashParts),
                timer: ''
            }
        },
        created: function () {

            this.fetchData();
            console.log("RR===" + this.refreshRate);
            this.timer = window.setInterval(this.fetchData, this.refreshRate)
        },
        filters: {
            duration(dur) {
                return moment.duration(dur, "milliseconds").format("h [hrs], m [min], ss [secs]");
            }
        },

        methods: {

            color(status) {
                var col;

                switch(status) {
                    case "passed":
                        col = "green";
                        break;
                    case "failed":
                        col = "red";
                        break;
                    case "running":
                        col = "blue";
                        break;
                    case "cancelled":
                        col = "darkgray";
                        break;
                    default:
                        col = "lightgray";
                }
                return col;
            },
            status(status) {
                if (status === "") {
                    return ""
                }
                return "(" + status + ")";
            },
            sed(name) {
                var rep = this.replacements[name];
                if (rep === undefined) {
                    return name;
                }
                return rep
            },
            goToEditor() {
                location.href = location.href.replace("/r", "/url-editor");
            },
            fetchData() {
                var xhr = new XMLHttpRequest();
                var self = this;
                console.log("l=" + location.pathname + "/" + self.code);
                xhr.open('GET', location.pathname + "/" + self.code);
                xhr.setRequestHeader("lastUpdated", self.rad.lastUpdated);
                xhr.onload = function () {
                    if (xhr.status === 200) {
                        var response = JSON.parse(xhr.responseText);

                        if ("undefined" === typeof response.message) {
                            self.rad = response;
                            self.rad.columnWidths = [];
                            self.rad.nonZeroCount = [];
                            self.rad.ips = self.rad.ips.join(", ");
                            for (var x = 0; x < self.rad.builds[0].steps.length; x++) {
                                self.rad.columnWidths.push(0);
                                self.rad.nonZeroCount.push(0);
                            }
                            for (var p = 0; p < self.rad.builds.length; p++) {
                                for (var q = 0; q < self.rad.builds[p].steps.length; q++) {
                                    // Expand step codes into longer names (if needed)
                                    self.rad.builds[p].steps[q].name = self.sed(self.rad.builds[p].steps[q].name);
                                    self.rad.columnWidths[q] += self.rad.builds[p].steps[q].dur;
                                    if (self.rad.builds[p].steps[q].dur > 0) {
                                        self.rad.nonZeroCount[q] += 1;
                                    }
                                }
                            }
                            // make averages
                            var total = 0;
                            for (var y = 0; y < self.rad.columnWidths.length; y++) {
                                self.rad.columnWidths[y] =  self.rad.columnWidths[y] / self.rad.nonZeroCount[y];
                                total += self.rad.columnWidths[y];
                            }
                            self.rad.minWidth = Math.round(total / 12);
                            total += self.rad.minWidth;

                            for (y = 0; y < self.rad.columnWidths.length; y++) {
                                self.rad.columnWidths[y] =  Math.round(self.rad.columnWidths[y] / total * 100);
                            }
                            self.rad.minWidth = Math.round(self.rad.minWidth / total * 100);


                        } else {
                            self.radiatorCodeCorrect = false;
                            self.egressIpAddress = response.egressIpAddress;
                        }
                    }
                };
                xhr.send()
            }
        },
        destroyed: function(){
            window.clearInterval(this.fetchData);
        }
    }
</script>
<style>
</style>

