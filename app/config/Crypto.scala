/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import com.google.inject._
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{ApplicationCrypto, CompositeSymmetricCrypto, Protected}

@Singleton
class Crypto @Inject()(appConfig: AppConfig, applicationCrypto: ApplicationCrypto) {

  implicit val symmetricCrypto: CompositeSymmetricCrypto = applicationCrypto.JsonCrypto

  def encrypt[A](data: A)(implicit wrt: Writes[A]): JsValue = {
    if(appConfig.mongodb.encryptionEnabled) {
      Json.toJson(Protected(data))(new JsonEncryptor[A]())
    } else {
      Json.toJson(data)
    }
  }

  def decrypt[A](json: JsValue)(implicit rds: Reads[A]): Option[A] = {
    if(appConfig.mongodb.encryptionEnabled) {
      json
        .validateOpt[Protected[A]](new JsonDecryptor[A]())
        .getOrElse(None)
        .map(_.decryptedValue)
    } else {
      json.asOpt
    }
  }
}