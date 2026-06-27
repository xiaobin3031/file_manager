// 底部事件栏
import { uploadFile } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'

class EventBar {
  constructor() {
    this.$dom = $('footer > div.event-bar')
    this.queue = []
    this.current = null
  }

  add(event) {
    event.attach(this)
    this.queue.push(event)
    this.next()
  }

  async next() {
    if(this.current) return
    const event = this.queue.shift()

    if(!event) return

    this.current = event
    this.refresh()

    try {
      await event.execute()
      await event.finish()
    }finally{
      this.current = null
      this.refresh()
      this.next()
    }
  }
  
  refresh() {
    if(!this.current) {
      this.$dom.innerHTML = ''
      return
    }

    this.$dom.innerHTML = `
      <span>${this.current.title}</span>
      <progress value="${this.current.progress}" max="100"></progress>
    `
  }
}

class BaseEvent {

  constructor(title) {
    this.title = title
    this.progress = 0
    this.status = 'waiting'
    this.bar = null
  }

  attach(bar) {
    this.bar = bar
  }

  setProgress(progress) {
    this.progress = progress
    this.refresh()
  }

  setTitle(title) {
    this.title = title
    this.refresh()
  }

  refresh() {
    this.bar?.refresh()
  }

  async execute() {}
  async finish(){}
}

export const eventBar = new EventBar()

export class UploadEvent extends BaseEvent {
  
  constructor(file) {
    super("上传: " + file.name)
    this.file = file
  }

  async execute() {
    await uploadFile(this.file, (percent) => {
      this.setProgress(Math.min(percent, 100).toFixed(0))
    })
  }
}
