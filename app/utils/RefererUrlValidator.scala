/*
 * Copyright 2022 HM Revenue & Customs
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

package utils

import config.AppConfig

object RefererUrlValidator {
  def isValid(refererUrl: String)(implicit config: AppConfig): Boolean =
    refersToServiceOnAllowList(refererUrl) && onlyContainsAllowedCharacters(refererUrl)

  private def onlyContainsAllowedCharacters(refererUrl: String): Boolean =
    refererUrl.matches("^[0-9A-Za-z/_?=&-]+$")

  private def refersToServiceOnAllowList(refererUrl: String)(implicit config: AppConfig): Boolean = {
    val urlNamespaceParts = refererUrl.split("/").toList
    urlNamespaceParts match {
      case first :: serviceName :: _ if (first.isEmpty) =>
        config.allowList.refererServices.contains(serviceName)
      case _ => false
    }
  }
}
