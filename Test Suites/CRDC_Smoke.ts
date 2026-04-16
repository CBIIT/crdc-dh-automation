<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>GC-focused UI smoke (no CTDC/ICDC): run after selecting a profile where G_Urlname matches the target Hub (e.g. CRDC_STAGE / CRDC_QA). Tests use Login.gov OTP keywords—ensure execution profiles define Submitter/DCP/Admin credentials and OTP as required by each script. API token tests (CRDCDH-1551/1526) are not included; use a separate job if needed. Order: EnableAllPermissions first (same pattern as CRDC_DSFlow), then Login, Submission Request, Data Submissions GC coverage.</description>
   <name>CRDC_Smoke</name>
   <tag>smoke</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>f100d0c0-dead-4000-a000-0000c0dec001</testSuiteGuid>
   <testCaseLink>
      <guid>46264ae5-b4c2-4190-bd84-cb7fbc09f594</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/Pbac/TC09_CRDC-PbacCheckedPermissions-Submitter</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>b0ab31ba-ca01-4384-af47-bc2c8dec8c0b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC12_SubmissionRequestsListAndFilter</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>9f291c07-f796-43a6-8835-d2ce3e92a732</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC10_DataSubmissionsListAndFilter</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>4e274902-eba3-4404-89de-25f47f638aa5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC08_ManageStudiesListAndFilter</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>13405e82-87a3-41ac-81ce-cb19bb466288</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC09_ManageProgramsListAndFilter</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>d96db697-65a6-499c-a230-b0fea3cb15ed</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC03_TableSortingVerification</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>f9b67898-3959-4251-a0ed-6ca31c5125d9</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC04_PaginationVerification</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>320eb111-9827-4838-a5ae-f1c5d356afa2</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/AuxiliaryTests/TC17_VisionScreenshotComparison</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a0000001-0001-4000-8000-000000000003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/SubmissionRequest/CRDCDH-332/TC01_CRDC_Submission_Request-AllFields</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a0000001-0001-4000-8000-000000000007</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-697/TC01_CRDC-VerifyDataSubmissionHeaders</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a0000001-0001-4000-8000-000000000009</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-1163/TC16_CRDC-1163-DCPValidationAfterSubmissionSubmittedMetadata</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a0000001-0001-4000-8000-00000000000a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-1051/TC25_CRDC-VerifyDcpCrossValidation</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a0000001-0001-4000-8000-00000000000d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-514/TC06_CRDC-514-VerifyFullDataSubmissionHistory</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a0000001-0001-4000-8000-00000000000f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-1035/TC101_CRDC-1035-VerifyValidationResultsExportAllRoles</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>f61c8628-e8be-4fc1-a5cb-04b235b7c52b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-887/TC101_CRDC-887-ColumnRequiredBatchErrors</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>3c6853af-f139-44ef-bb75-b00dc87b603e</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-2864/TC102_CRDC-2864-BatchErrorsCompositeIdRequiredProperty</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>26e1e720-1db8-427b-a4c1-4306fe9f1051</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/CRDC/DataSubmissions/GC/CRDCDH-1405/TC101_CRDC-1405-CLINoFileIdVerifyFinalManifest</testCaseId>
      <usingDataBindingAtTestSuiteLevel>true</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
