const CHUNK_SIZE = 20 * 1024 * 1024; // 每片 xMB

const baseUrl = "http://192.168.50.133:6547"

async function request(url, options = {}) {
    const token = `Bearer ${localStorage.getItem('token') || ''}`
    let requestUrl = baseUrl + url;

    const method = (options.method || 'GET').toUpperCase();

    if (method === 'GET' && options.body) {
        const params = new URLSearchParams(options.body).toString();
        requestUrl += (requestUrl.includes('?') ? '&' : '?') + params;
    }

    const isFormData = options.body instanceof FormData
    const headers = {
      Authorization: token,
      ...options.headers
    }
    if (!isFormData) {
      headers['Content-Type'] = 'application/json'
    }
    const config = {
      method,
      headers
    };

    if (method !== 'GET' && options.body) {
      config.body = isFormData
          ? options.body
          : JSON.stringify(options.body)
    }

    const res = await fetch(requestUrl, config);
    if(!res.ok) {
        throw new Error('请求失败')
    }
    const rJson = await res.json()
    console.log("rJson", rJson)
    if(rJson.code !== 0) {
      if(rJson.msg == "not login") {
        window.location.replace('/login.html')
        return
      }
      throw new Error(rJson.message || '请求失败')
    }
    return rJson.data
}


async function uploadFile(file, onProgress) {
  const total = file.size
  const totalChunks = Math.ceil(total / CHUNK_SIZE)
  let {currentChunk, uploadId} = await request('/file-upload/init', {
    method: 'POST',
    body: {
      totalChunks,
      fileName: file.name,
      totalSize: total
    }
  })

  while(currentChunk <= totalChunks) {
    const start = currentChunk * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, total)
    if(start >= end) break
    const blob = file.slice(start, end)
    const formData = new FormData()
    formData.append('file', blob)
    formData.append('fileId', uploadId);
    formData.append('currentChunk', currentChunk + "");
    formData.append('totalChunks', totalChunks + "");

    const res = await uploadFileChunk(formData, onProgress, currentChunk, totalChunks)
    if(res === 1) {
      currentChunk++
    }else{
      throw new Error('上传失败')
    }
  }

  await request('/file-upload/finish', {
    method: "POST",
    body: {fileId: uploadId}
  })
}

async function uploadFileChunk(formData, onProgress, currentChunk, totalChunks) {
    return new Promise(resolve => {

        const token = `Bearer ${localStorage.getItem('token') || ''}`
        const xhr = new XMLHttpRequest();
        let url = `${baseUrl}/file-upload/upload`
        xhr.open('POST', url, true);
        xhr.setRequestHeader('Authorization', token);

        xhr.onload = function () {
            if (xhr.status === 200) {
                resolve(1)
            } else {
                console.error('上传失败', xhr.responseText);
                resolve(0)
            }
        };

        const curPercent = currentChunk * 100 / totalChunks
        xhr.upload.onprogress = function (e) {
            if (e.lengthComputable) {
                // 这里需要计算当前百分比，已完成的百分比 + 当前分片占所有分片的百分比
                onProgress(curPercent + e.loaded * 100 / e.total / totalChunks)
            }else{
                onProgress(-1, -1)
            }
        };

        xhr.onerror = function () {
            console.error('网络错误');
            resolve(0)
        };
        xhr.send(formData);
    })
}

