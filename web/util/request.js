import { ENV } from "../config/env.js"

const CHUNK_SIZE = 10 * 1024 * 1024; // 每片 xMB

export const baseUrl = ENV.BASE_URL

export async function downloadFile(fileId) {
  const fileToken = await request('/ftp/prepareFile', {
    method: "POST",
    body: {
      id: fileId
    }
  })
  if(!fileToken) {
    window.alert("文件准备失败")
    return
  }
  let requestUrl = `${ENV.BASE_URL}/ftp/downloadFile?fileToken=${fileToken}`;
  window.open(requestUrl)
}

export async function request(url, options = {}) {
    console.log(ENV.BASE_URL)
    const token = `Bearer ${localStorage.getItem('token') || ''}`
    let requestUrl = ENV.BASE_URL + url;

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
      window.alert("请求失败")
      throw new Error('请求失败')
    }
    const rJson = await res.json()
    if(ENV.DEBUG) {
      console.log("rJson", rJson)
    }
    if(rJson.code !== 0) {
      if(rJson.msg == "not login") {
        window.location.replace('/login.html')
        return
      }
      window.alert(rJson.message || '请求失败')
      throw new Error(rJson.message || '请求失败')
    }
    return rJson.data
}


export async function uploadFile(data, onProgress) {
  let file = data.file
  let foldId = data.foldId
  const total = file.size
  const totalChunks = Math.ceil(total / CHUNK_SIZE)
  let {currentChunk, uploadId} = await request('/file-upload/init', {
    method: 'POST',
    body: {
      foldId,
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
        let url = `${ENV.BASE_URL}/file-upload/upload`
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
