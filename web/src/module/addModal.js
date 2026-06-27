import { $, $$ } from '#utils/dom.js'
import { is_enter } from '#utils/key_event.js'
import { request, uploadFile, baseUrl } from '#utils/request.js'
import { buildFileDom } from '#modules/fileDom.js'
import { showModal, hideModal } from '#components/modal.js'
import { sleep } from '#utils/time.js'
import { loadDirs, getFiles } from '#modules/file.js'
import { UploadEvent, eventBar } from '#modules/eventBar.js'

let $modal = null

export const appendAddModal = () => {
  $modal = showModal('新建', buildAddModalBody(), null)
}

const buildAddModalBody = () => {
  const $body = document.createElement('div')
  $body.id = 'file-add-body'

  const bodyText = `
    <div>
        <label><input type="radio" name="addType" value="1" checked="true" />文件夹</label>
        <label><input type="radio" name="addType" value="2" />文件</label>
        <label><input type="radio" name="addType" value="3" />下载任务</label>
        <label><input type="radio" name="addType" value="4" />下载计划</label>
    </div>
    <div>
        <div class="tab-item tab-1 active">
            <input type="text" placeholder="请输入文件夹名称" />
        </div>
        <div class="tab-item tab-2">
            <div> <input type="file" multiple="multiple" /><button type="button">上传</button></div>
            <div class="file-add-info">
                <span></span>
                <span></span>
                <span class="filename"></span>
            </div>
            <div class="file-add-progress">
                <div class="progress-bar"></div>
                <span class="progress-text">0</span>
            </div>
        </div>
        <div class="tab-item tab-3">
          <div> <textarea placeholder="请输入下载链接"></textarea> </div>
          <div><input type="file" /> <button type="button">上传</button></div>
        </div>
        <div class="tab-item tab-4">
            <div class="download-url-input">
                <input type="text" placeholder="请输入下载网址"/>
                <button type="button">测试</button>
            </div>
            <div class="xpath-input">
              <textarea rows=3 placeholder="请输入xpath表达式"></textarea>
            </div>
            <div class="magnet-list"></div>
        </div>
    </div>
  `

  $body.innerHTML = bodyText

  $('.tab-1 > input', $body).addEventListener('keyup', async (e) => {
    if(is_enter(e, {})) {
      let val = e.target.value
      if(!val || !val.trim()) {
        return
      }
      val = val.trim()
      const foldId = await request('/ftp/addFold', {
        method: "POST",
        body: {
          dirName: val
        }
      })
      if(!!foldId) {
        const fold = {id: foldId, fileFlag: false, name: val}
        const $dom = buildFileDom(fold)
        $('#app').prepend($dom)
        getFiles().unshift(fold)
        e.target.value = ''
        hideModal($modal)
      }
    }
  })

  Array.from($$('input[name="addType"]', $body)).forEach((input) => {
    input.addEventListener('change', (e) => {
      const type = e.target.value
      $('#file-add-body .tab-item.active')?.classList.remove('active')
      $(`#file-add-body .tab-item.tab-${type}`).classList.add('active')
    })
  })

  $('.tab-2 input[type="file"]', $body).addEventListener('change', (e) => {
    const files = e.target.files
    const $filename = $('.file-add-info span.filename', $body)
    const $spans = $$('.file-add-info span', $body)
    if(files.length === 0) {
      $spans[0].innerText = ''
      $filename.innerText = ''
      return
    }
    $spans[0].innerText = `1 / ${files.length}`
    $filename.innerText = `${files[0].name}`
    $('.tab-2', $body).classList.add('wait')
  })

  // 上传
  $('.tab-2 button', $body).addEventListener('click', async (e) => {
    const files = $body.querySelector('.tab-2 input[type="file"]').files
    if(files.length === 0) {
      alert('请选择文件')
      return
    }
    const $progressBar = $('.file-add-progress .progress-bar', $body)
    const $progressText = $('.file-add-progress .progress-text', $body)
    const $progress = $('.file-add-progress', $body)
    $progressBar.style.width = '0'
    const $tab2 = $('.tab-2', $body)
    const $filename = $('.file-add-info span.filename', $body)
    for(let file of files) {
      eventBar.add(new UploadEvent(file))
    }
    /*for(let i=0;i<files.length;i++) {
      const file = files[i]
      const $spans = $$('.file-add-info span', $body)
      $spans[0].innerText = `${i + 1} / ${files.length}`
      $filename.innerText = `${file.name}`
      $tab2.classList.replace('wait', 'uploading')
      await uploadFile(file, (percent) => {
          if(percent === -1) return
          let pp = Math.min(percent, 100).toFixed(0)
          $progressBar.style.width = `${pp}%`
          $progressText.innerText = `${pp}`
          if (+pp === 100) {
              $tab2.classList.replace('uploading', 'success')
          }
      })
      await sleep()
      $tab2.classList.replace('success', 'wait')
      $progressBar.style.width = '0'
      $progressText.innerText = '0'
      await sleep()
    }*/

    loadDirs()
    hideModal($modal)
  })

  $('.tab-3 textarea', $body).addEventListener('keyup', async (e) => {
    if(is_enter(e, {ctrl: true})) {
      let val = e.target.value
      if(!val || !val.trim()) return
      val = val.trim()
      const file = await request('/ftp/addDownload', {
        method: "POST",
        body: {
          magnet: val
        }
      })
      getFiles().push(file)
      e.target.value = ''
      $('#app').appendChild(buildFileDom(file))
    }
  })

  $('.tab-3 button', $body).addEventListener('click', async(e) => {
    const $input = e.target.previousElementSibling
    const files = $input.files
    if(files.length === 0) {
      return
    }
    const file = files[0]
    if(!file.name.endsWith('.torrent')) {
      return
    }
    const formData = new FormData()
    formData.append('file', file)
    formData.append('name', file.name)
    await request('/ftp/addDownloadTorrent', {
      method: "POST",
      body: formData
    })
    $input.value = ''
  })

  $('.tab-4 .download-url-input button', $body).addEventListener('click', async(e) => {
    const val = e.currentTarget.previousElementSibling.value
    const xpath_val = $('.tab-4 .xpath-input textarea').value
    if(!val.trim() || !xpath_val?.trim()) return
    val = val.trim()
    let html = await request('/ftp/loadHtmlText', {
      method: "GET",
      body: {
        url: val
      }
    })
    if (!!html) {
      const parser = new DOMParser()
      const doc = parser.parseFromString(html, 'text/html')
      const xpathResult = document.evaluate(xpath_val, doc, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)
      let results = []
      for(let i=0;i<xpathResult.snapshotLength;i++) {
        result.push(xpathResult.snapshotItem(i).textContent.trim())
      }
      const $magnetList = $('.tab-4 .magnet-list', $body)
      let $dom = document.createElement('div')
      $dom.innerText(`total: ${results.length}`)
      $magnetList.appendChild($dom)
      for(let uu of results) {
        $dom = document.createElement('div')
        $dom.innerText = uu
        $magnetList.appendChild($dom)
      }
    }
  })
  
  return $body
}
