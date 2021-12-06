System.properties['citrus.mail.marshaller.type'] = "JSON"

mail()
  .server('mail-server')
  .port(22222)
  .autoStart(true)
