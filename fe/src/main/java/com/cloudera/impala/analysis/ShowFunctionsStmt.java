// Copyright 2012 Cloudera Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.impala.analysis;

import com.cloudera.impala.authorization.Privilege;
import com.cloudera.impala.common.AnalysisException;
import com.cloudera.impala.thrift.TFunctionType;
import com.cloudera.impala.thrift.TShowFunctionsParams;
import com.google.common.base.Preconditions;

/**
 * Representation of a SHOW FUNCTIONS [pattern] statement.
 * Acceptable syntax:
 *
 * SHOW FUNCTIONS
 * SHOW FUNCTIONS LIKE 'pattern'
 *
 */
public class ShowFunctionsStmt extends StatementBase {
  // Pattern to match tables against. | denotes choice, * matches all strings
  private final String pattern_;

  // DB (if any) as seen by the parser
  private final String parsedDb_;

  // If true, show UDAs, otherwise, show UDFs
  private final boolean isAggregate_;

  // Set during analysis
  private String postAnalysisDb_;

  /**
   * Constructs a show statement which matches all functions against the
   * supplied pattern.
   */
  public ShowFunctionsStmt(String db, String pattern, boolean isAggregate) {
    parsedDb_ = db;
    pattern_ = pattern;
    isAggregate_ = isAggregate;
  }

  /**
   * Can only be called after analysis, returns the name of the database that
   * this show will search against.
   */
  public String getDb() {
    Preconditions.checkNotNull(postAnalysisDb_);
    return postAnalysisDb_;
  }

  public String getPattern() { return pattern_; }

  @Override
  public String toSql() {
    if (pattern_ == null) {
        return "SHOW FUNCTIONS";
    } else {
        return "SHOW FUNCTIONS LIKE '" + pattern_ + "'";
    }
  }

  @Override
  public void analyze(Analyzer analyzer) throws AnalysisException {
    postAnalysisDb_ = (parsedDb_ == null ? analyzer.getDefaultDb() : parsedDb_);
    if (analyzer.getDb(postAnalysisDb_, Privilege.VIEW_METADATA) == null) {
      throw new AnalysisException(Analyzer.DB_DOES_NOT_EXIST_ERROR_MSG + postAnalysisDb_);
    }
  }

  public TShowFunctionsParams toThrift() {
    TShowFunctionsParams params = new TShowFunctionsParams();
    params.setType(isAggregate_ ? TFunctionType.AGGREGATE : TFunctionType.SCALAR);
    params.setDb(getDb());
    params.setShow_pattern(getPattern());
    return params;
  }
}
