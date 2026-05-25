import { $, $$ } from '#utils/dom'
import { getFiles } from '#pages/file'
import { request } from '#utils/request'

export const buildFileDom = (file) => {
    const $file = document.createElement('div')
    $file.classList.add('file-item')
    $file.draggable = true
    $file.dataset.fileId = file.id
    $file.dataset.fileId = file.id
    $file.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('dragFileId', e.currentTarget.dataset.fileId)
    })
    $file.addEventListener('drop', async (e) => {
        const dragFileId = +e.dataTransfer.getData('dragFileId')
        const fileId = +e.currentTarget.dataset.fileId
        if(dragFileId == fileId) return
        const currentFiles = getFiles()
        let idx = currentFiles.findIndex(item => item.id === +dragFileId)
        if(idx === -1) return
        const [movedItem] = currentFiles.splice(idx, 1)
        currentFiles.splice(idx, 0, movedItem)

        const body = currentFiles.map((item, index) => ({
            id: item.id,
            fileFlag: arguments.fileFlag,
            sort: item.sort
          }))
        await request('/ftp/sortFiles', {
          method: "POST",
          body: body
        })
    })
    $file.addEventListener('dragover', (e) => {
        e.preventDefault()
    })

    const bodyText = `
        <div class="file-sample"></div>
        <div class="file-info">
            <div class="file-name">${file.name}</div>
        </div>
    `
    $file.innerHTML = bodyText

    const $sample = $('.file-sample', $file)
    $sample.addEventListener('click', (e) => {
        e.currentTarget.parentNode.classList.toggle('selected')
        changeCheckOpsStatus()
    })
    $sample.addEventListener('dblclick', (e) => {
        dblClickFile(file)
    })

    if(file.fileFlag) {
        $sample.style.backgroundImage = `url("${baseUrl}/file/sample/${file.id}.webp")`
    } 
    else $file.classList.add('dir')
    return $file
}

const dblClickFile = async (file) => {
    if(file.fileFlag) {
        if(isVideo(file)) {
          // todo 视频预览
        }else if(isImage(file)) {
          // todo 图片预览
        }else if(isPdf(file)) {
          // todo pdf预览
        }
    }else {
      const res = await request(`/ftp/changeDir`, {
        method: "POST",
        body: {
          id: file.id
        }
      })
      let showDefault = true
      if(!!res.lastFile) {
          if(window.confirm('有观看历史，是否进入？')) {
            showDefault = false
            dblClickFile(res.lastFile)
          }
      }
      if(showDefault) {
          refreshDirs(res)
      }
    }
}

const isVideo = (file) => {
    return !!file.fileType && file.fileType.indexOf("video/") === 0;
}
const isImage = (file) => {
    return !!file.fileType && file.fileType.indexOf("image/") === 0;
}
const isPdf = (file) => {
    return !!file.fileType && file.fileType === 'application/pdf';
}

const changeAddType = (type) => {
    $('#file-add-body .tab-item.active')?.classList.remove('active')
    $(`#file-add-body .tab-item.tab-${type}`).classList.add('active')
}
