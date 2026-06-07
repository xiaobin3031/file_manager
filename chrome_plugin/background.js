chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if(msg.type === 'upload_tmp') {
    // fetch('http://127.0.0.1:6547/ftp/addTmpFile', {
    fetch('http://192.168.50.133:6547/ftp/addTmpFile', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(msg.data)
    })
    .then(r => r.json())
    .then(sendResponse)

    return true
  }
})
