const streamDownloadToFile = async (response, fileName) => {
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let result = "";

    while(true){
        const { done, value } = await reader.read();
        if(done) break;
        if(value){
            result += decoder.decode(value, {stream: true});
        }
    }
    // flush remaining
    result += decoder.decode();

    const blob = new Blob([result], { type: "text/plain" });
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    document.body.appendChild(anchor);

    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(url);
}


export default streamDownloadToFile;