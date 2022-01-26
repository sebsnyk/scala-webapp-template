package net.wiringbits.modules

import com.google.inject.AbstractModule
import net.wiringbits.apis.{EmailApi, EmailApiAWSImpl}

class ApisModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[EmailApi]).to(classOf[EmailApiAWSImpl])
  }
}
