/**
 * Creates Discofi Android XML translations based on Discord J2ME JSON translations.
 * Inserts old translations into the XML translation template.
 */

const fs = require('fs');
const { default: stripJsonComments } = require('strip-json-comments');

const langTemplate = fs.readFileSync("language_template.xml").toString();

const j2meTransNames = fs.readdirSync("j2me");

// English strings to use as fallbacks
const j2meStringsEn = JSON.parse(stripJsonComments(fs.readFileSync("j2me/en.jsonc").toString()));

j2meTransNames.forEach((filename) => {

    if (filename == 'en.jsonc') return;

    // -- language name parse --------------------------------------

    let langName, langRegion;

    filename.replace(/^(\w+)\-?(\w+)?/, (_, name, region) => {
        langName = name;
        langRegion = region;
    })

    if (!langName) throw new Error("no langname: " + filename);

    console.log("[" + langName + "], [" + langRegion + "]");


    // -- language template --------------------------------------

    const j2meStrings = JSON.parse(stripJsonComments(fs.readFileSync("j2me/" + filename).toString()));

    const langTemplateOut = langTemplate.replace(/§(\d+)§/g, (_, index) => {
        index = Number(index);

        if (isNaN(index) || index < 0 || index >= j2meStrings.length) {
            throw new Error("invalid index " + index);
        }

        let result = j2meStrings[index] ?? j2meStringsEn[index];

        return result.replaceAll("&", "&amp;").replaceAll("'", "\\&apos;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
    })

    // -- output --------------------------------------

    const outDir = "../app/src/main/res/values-" +
        langName +
        (langRegion ? ("-r" + langRegion) : "")

    const outFilename = outDir + "/strings.xml";

    try { fs.mkdirSync(outDir, {recursive: true}); } catch (e) {}

    fs.writeFileSync(outFilename, langTemplateOut);
    console.log("wrote " + outFilename);
})