<template>
    <div>
        <h2>Editor, code, title and steps</h2>

        <div>Radiator Code: {{ locn.code }}</div>
        <br/>
        <div>Page Title: <input v-model="locn.title" placeholder="title"></div>
        <br/>
        <div>Step codes and corresponding descriptions:</div>
        <ol>
            <li style="width: 100%" v-for="replNum in ((locn.replacements.length /2))">
                <span>
                    <span>{{ locn.replacements[(replNum-1)*2]}}: <span>
                    <input v-model="locn.replacements[((replNum-1)*2)+1]" placeholder="description">
                </span>
            </li>
        </ol>

        <button v-on:click="all_done">Done</button>

        <pre>
            <code>
                {{ locn }}
            </code>
        </pre>
    </div>
</template>
<script>

    var lhash = location.hash.replace(new RegExp("_", 'g'), " ");
    var hashParts = lhash.substr(1).split("/");
    console.log("---> dddd")

    module.exports = {
        data: function() {
            return {
                locn: {
                code: hashParts.shift(),
                title: hashParts.shift(),
                replacements: hashParts
                }
            }
        },
        methods: {
            all_done(event) {
                location.href = ("/r#" + this.locn.code + "/" + this.locn.title + "/" + this.locn.replacements.join("/")).replace(new RegExp(" ", 'g'), "_");
            }
        }
    }
</script>
<style>
</style>

